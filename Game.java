import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game extends JPanel implements Runnable, WebSocketClient.Listener {
    public static final int SCREEN_WIDTH = 960, SCREEN_HEIGHT = 640;
    // Change this when connecting to a server on another machine.
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private final Input input = new Input();
    private final World world = new World();
    private final Map<Integer, RemotePlayer> players = new ConcurrentHashMap<>();
    private final Map<Long, Point> bullets = new ConcurrentHashMap<>();
    private final java.util.List<Point> loot = Collections.synchronizedList(new ArrayList<>());

    private WebSocketClient socket;
    private volatile int myId = -1;
    private volatile String connectionStatus = "Connecting...";
    private Thread gameThread;
    private int cameraX, cameraY;

    public Game() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        addKeyListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);

        try {
            socket = new WebSocketClient(SERVER_HOST, SERVER_PORT, this);
            socket.connect();
        } catch (Exception e) {
            connectionStatus = "Connection failed: " + e.getMessage();
        }
    }

    public void start() {
        requestFocusInWindow();
        gameThread = new Thread(this, "render-loop");
        gameThread.start();
    }

    @Override public void run() {
        long next = System.currentTimeMillis();
        while (true) {
            long now = System.currentTimeMillis();
            if (now >= next) {
                sendInput();
                next += 16;
                if (now - next > 1000) next = now;
            }
            repaint();
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
    }

    private void sendInput() {
        if (socket == null || myId < 0) return;
        boolean w = input.isKeyDown(KeyEvent.VK_W);
        boolean s = input.isKeyDown(KeyEvent.VK_S);
        boolean a = input.isKeyDown(KeyEvent.VK_A);
        boolean d = input.isKeyDown(KeyEvent.VK_D);

        RemotePlayer me = players.get(myId);
        if (me == null) return;

        double mouseWorldX = input.getMouseX() + cameraX;
        double mouseWorldY = input.getMouseY() + cameraY;

        try {
            socket.sendText("INPUT|" + bit(w) + "|" + bit(s) + "|" + bit(a) + "|" + bit(d)
                    + "|" + bit(input.isMouseDown()) + "|" + mouseWorldX + "|" + mouseWorldY);
        } catch (Exception ignored) {}
    }

    private int bit(boolean b) { return b ? 1 : 0; }

    @Override public void onText(String text) {
        String[] parts = text.split("\\|", -1);
        if (parts.length == 0) return;

        if (parts[0].equals("WELCOME")) {
            myId = Integer.parseInt(parts[1]);
            connectionStatus = "Connected";
            return;
        }

        if (!parts[0].equals("STATE") || parts.length < 5) return;

        Map<Integer, RemotePlayer> nextPlayers = new HashMap<>();
        if (!parts[2].isEmpty()) {
            for (String row : parts[2].split(";")) {
                String[] p = row.split(",");
                if (p.length < 6) continue;
                int id = Integer.parseInt(p[0]);
                nextPlayers.put(id, new RemotePlayer(
                    id, Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                    Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])
                ));
            }
        }
        players.clear();
        players.putAll(nextPlayers);

        bullets.clear();
        if (!parts[3].isEmpty()) {
            for (String row : parts[3].split(";")) {
                String[] p = row.split(",");
                if (p.length >= 3)
                    bullets.put(Long.parseLong(p[0]),
                        new Point(Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            }
        }

        loot.clear();
        if (!parts[4].isEmpty()) {
            for (String row : parts[4].split(";")) {
                String[] p = row.split(",");
                if (p.length >= 2) loot.add(new Point(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            }
        }
    }

    @Override public void onClose() {
        connectionStatus = "Disconnected";
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RemotePlayer me = players.get(myId);
        if (me != null) {
            cameraX = (int)me.x - SCREEN_WIDTH / 2;
            cameraY = (int)me.y - SCREEN_HEIGHT / 2;
            cameraX = Math.max(0, Math.min(cameraX, world.getWidth() - SCREEN_WIDTH));
            cameraY = Math.max(0, Math.min(cameraY, world.getHeight() - SCREEN_HEIGHT));
        }

        world.draw(g, cameraX, cameraY, SCREEN_WIDTH, SCREEN_HEIGHT);

        g.setColor(new Color(240,190,60));
        synchronized (loot) {
            for (Point p : loot)
                g.fillRect(p.x - cameraX - 7, p.y - cameraY - 7, 14, 14);
        }

        g.setColor(Color.YELLOW);
        for (Point p : bullets.values())
            g.fillOval(p.x - cameraX - 3, p.y - cameraY - 3, 6, 6);

        for (RemotePlayer p : players.values()) drawPlayer(g, p);
        drawUI(g, me);

        g.dispose();
    }

    private void drawPlayer(Graphics2D g, RemotePlayer p) {
        int sx = (int)p.x - cameraX, sy = (int)p.y - cameraY;
        g.setColor(p.id == myId ? new Color(55,140,220) : new Color(220,75,180));
        g.fillOval(sx - 16, sy - 16, 32, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(sx - 3, sy - 24, 6, 15);

        g.setColor(Color.RED);
        g.fillRect(sx - 20, sy - 28, 40, 5);
        g.setColor(Color.GREEN);
        g.fillRect(sx - 20, sy - 28, 40 * Math.max(0, p.hp) / 100, 5);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String label = "P" + p.id;
        g.drawString(label, sx - g.getFontMetrics().stringWidth(label) / 2, sy + 34);
    }

    private void drawUI(Graphics2D g, RemotePlayer me) {
        g.setColor(new Color(0,0,0,175));
        g.fillRect(15, 15, 300, 112);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        if (me != null) {
            g.drawString("HP: " + Math.max(0, me.hp), 30, 40);
            g.drawString("Ammo: " + me.ammo, 30, 65);
            g.drawString("Loot: " + me.loot, 30, 90);
            g.drawString("Players: " + players.size(), 30, 115);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.drawString(connectionStatus, 700, 30);
        g.drawString("WASD move | Mouse shoot", 700, 48);

        if (me != null && me.hp <= 0) {
            g.setColor(new Color(0,0,0,190));
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            String t = "YOU DIED";
            g.drawString(t, (SCREEN_WIDTH - g.getFontMetrics().stringWidth(t))/2, SCREEN_HEIGHT/2);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            String s = "You can reconnect by restarting the client.";
            g.drawString(s, (SCREEN_WIDTH - g.getFontMetrics().stringWidth(s))/2, SCREEN_HEIGHT/2 + 40);
        }
    }

    private static class RemotePlayer {
        final int id, hp, ammo, loot;
        final double x, y;
        RemotePlayer(int id, double x, double y, int hp, int ammo, int loot) {
            this.id=id; this.x=x; this.y=y; this.hp=hp; this.ammo=ammo; this.loot=loot;
        }
    }
}
