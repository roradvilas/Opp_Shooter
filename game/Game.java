package game;

import bridge.PlayerType;
import facade.BulletView;
import facade.ClientFacade;
import facade.GameState;
import facade.PlayerInput;
import facade.PlayerView;
import observer.PlayerEvent;
import strategy.WeaponType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;

public class Game extends JPanel implements Runnable, ClientFacade.Listener {
    public static final int SCREEN_WIDTH = 960, SCREEN_HEIGHT = 640;
    // Change this when connecting to a server on another machine.
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private final Input input = new Input();
    private volatile World world = new WorldBuilder().build();
    private volatile String notification = "";
    private volatile long notificationUntil;
    private volatile Map<Integer, PlayerView> players = Map.of();
    private volatile java.util.List<BulletView> bullets = java.util.List.of();
    private volatile java.util.List<Loot> loot = java.util.List.of();
    private final JPanel spawnScreen = new JPanel(null) {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(0, 0, 0, 210));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private final JLabel spawnTitle = new JLabel("READY TO PLAY", SwingConstants.CENTER);
    private final JComboBox<PlayerType> classSelection = new JComboBox<>(PlayerType.values());
    private final JComboBox<WeaponType> weaponSelection = new JComboBox<>(WeaponType.values());
    private final JButton spawnButton = new JButton("Spawn");
    private volatile boolean alive;
    private volatile boolean spawnPending;

    private volatile ClientFacade server;
    private volatile int myId = -1;
    private volatile String connectionStatus = "Connecting...";
    private Thread gameThread;
    private volatile int cameraX, cameraY;

    public Game() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setLayout(null);
        setFocusable(true);
        addKeyListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { input.reset(); }
        });
        spawnScreen.setOpaque(false);
        spawnScreen.setBounds(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        spawnTitle.setForeground(Color.WHITE);
        spawnTitle.setFont(new Font("Arial", Font.BOLD, 48));
        spawnTitle.setBounds(160, 140, 640, 70);
        spawnScreen.add(spawnTitle);
        addSelectionLabel("Player class", 235);
        classSelection.setBounds(310, 265, 340, 35);
        classSelection.setSelectedItem(PlayerType.NORMAL);
        spawnScreen.add(classSelection);
        addSelectionLabel("Weapon", 315);
        weaponSelection.setBounds(310, 345, 340, 35);
        spawnScreen.add(weaponSelection);
        JLabel rates = new JLabel("Rifle: 100% rate | Pistol: 40% | Shotgun: 20%, 3 pellets", SwingConstants.CENTER);
        rates.setForeground(Color.LIGHT_GRAY);
        rates.setBounds(180, 390, 600, 30);
        spawnScreen.add(rates);
        spawnButton.setBounds(380, 445, 200, 45);
        spawnButton.setEnabled(false);
        spawnButton.addActionListener(e -> requestSpawn());
        spawnScreen.add(spawnButton);
        add(spawnScreen);
    }

    private void addSelectionLabel(String text, int y) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setBounds(310, y, 340, 25);
        spawnScreen.add(label);
    }

    private void requestSpawn() {
        if (server == null || myId < 0 || alive || spawnPending) return;
        spawnPending = true;
        spawnButton.setEnabled(false);
        input.reset();
        try {
            PlayerType type = (PlayerType)classSelection.getSelectedItem();
            WeaponType weapon = (WeaponType)weaponSelection.getSelectedItem();
            server.spawn(type, weapon);
        } catch (Exception e) {
            spawnPending = false;
            connectionStatus = "Spawn failed: " + e.getMessage();
            spawnButton.setEnabled(true);
        }
    }

    public void start() {
        requestFocusInWindow();
        gameThread = new Thread(this, "render-loop");
        gameThread.start();
    }

    @Override public void run() {
        try {
            server = new ClientFacade(SERVER_HOST, SERVER_PORT, this);
            server.connect();
        } catch (Exception e) {
            connectionStatus = "Connection failed: " + e.getMessage();
            if (server != null) server.close();
        }
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
        if (server == null || myId < 0 || !alive) return;
        boolean w = input.isKeyDown(KeyEvent.VK_W);
        boolean s = input.isKeyDown(KeyEvent.VK_S);
        boolean a = input.isKeyDown(KeyEvent.VK_A);
        boolean d = input.isKeyDown(KeyEvent.VK_D);

        PlayerView me = players.get(myId);
        if (me == null) return;

        double mouseWorldX = input.getMouseX() + cameraX;
        double mouseWorldY = input.getMouseY() + cameraY;

        try {
            server.sendInput(new PlayerInput(w, s, a, d, input.isMouseDown(),
                    mouseWorldX, mouseWorldY, input.movementType(), null));
        } catch (Exception ignored) {}
    }

    @Override public void onWelcome(int playerId, long worldSeed) {
        myId = playerId;
        world = new WorldBuilder().seed(worldSeed).build();
        connectionStatus = "Connected";
        SwingUtilities.invokeLater(() -> spawnButton.setEnabled(true));
    }

    @Override public void onSpawned() {
        notification = "";
        alive = true;
        spawnPending = false;
        SwingUtilities.invokeLater(() -> {
            spawnScreen.setVisible(false);
            input.reset();
            requestFocusInWindow();
        });
    }

    @Override public void onDied() {
        alive = false;
        spawnPending = false;
        Map<Integer, PlayerView> remaining = new HashMap<>(players);
        remaining.remove(myId);
        players = Map.copyOf(remaining);
        SwingUtilities.invokeLater(() -> {
            input.reset();
            spawnTitle.setText("YOU DIED");
            spawnTitle.setForeground(Color.RED);
            spawnButton.setText("Respawn");
            spawnButton.setEnabled(true);
            spawnScreen.setVisible(true);
        });
    }

    @Override public void onEvent(PlayerEvent event) {
        notification = event.message();
        notificationUntil = System.currentTimeMillis() + 3000;
    }

    @Override public void onState(GameState state) {
        players = state.players();
        bullets = state.bullets();
        loot = state.loot();
    }

    @Override public void onDisconnected() {
        connectionStatus = "Disconnected";
        alive = false;
        SwingUtilities.invokeLater(() -> {
            spawnTitle.setText("DISCONNECTED");
            spawnButton.setEnabled(false);
            spawnScreen.setVisible(true);
        });
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        PlayerView me = players.get(myId);
        if (me != null) {
            cameraX = (int)me.x() - SCREEN_WIDTH / 2;
            cameraY = (int)me.y() - SCREEN_HEIGHT / 2;
            cameraX = Math.max(0, Math.min(cameraX, world.getWidth() - SCREEN_WIDTH));
            cameraY = Math.max(0, Math.min(cameraY, world.getHeight() - SCREEN_HEIGHT));
        }

        world.draw(g, cameraX, cameraY, SCREEN_WIDTH, SCREEN_HEIGHT);

        for (Loot p : loot) {
            int sx = p.x() - cameraX, sy = p.y() - cameraY;
            if (p.type() == LootType.AMMO) {
                g.setColor(new Color(240, 190, 60));
                g.fillRect(sx - 7, sy - 7, 14, 14);
            } else if (p.type() == LootType.HEALTH) {
                g.setColor(new Color(65, 200, 100));
                g.fillRect(sx - 8, sy - 8, 16, 16);
                g.setColor(Color.WHITE);
                g.fillRect(sx - 2, sy - 6, 4, 12);
                g.fillRect(sx - 6, sy - 2, 12, 4);
            } else {
                g.setColor(p.type() == LootType.RAPID_FIRE ? new Color(70, 140, 235) : new Color(220, 60, 60));
                g.fillRect(sx - 8, sy - 8, 16, 16);
            }
        }

        g.setColor(Color.YELLOW);
        for (BulletView b : bullets)
            g.fillOval(b.x() - cameraX - 3, b.y() - cameraY - 3, 6, 6);

        for (PlayerView p : players.values()) drawPlayer(g, p);
        drawUI(g, me);

        g.dispose();
    }

    private void drawPlayer(Graphics2D g, PlayerView p) {
        if (p.hp() <= 0) return;
        int sx = (int)p.x() - cameraX, sy = (int)p.y() - cameraY;
        g.setColor(p.id() == myId ? new Color(55,140,220) : new Color(220,75,180));
        g.fillOval(sx - 16, sy - 16, 32, 32);

        double angle = p.angle();
        if (p.id() == myId) angle = Math.atan2(input.getMouseY() - sy, input.getMouseX() - sx);
        Graphics2D weaponGraphics = (Graphics2D)g.create();
        weaponGraphics.translate(sx, sy);
        weaponGraphics.rotate(angle);
        weaponGraphics.setColor(Color.DARK_GRAY);
        weaponGraphics.fillRect(8, -3, 20, 6);
        weaponGraphics.dispose();

        g.setColor(Color.RED);
        g.fillRect(sx - 20, sy - 28, 40, 5);
        g.setColor(Color.GREEN);
        g.fillRect(sx - 20, sy - 28, 40 * Math.max(0, p.hp()) / p.maxHealth(), 5);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String label = "P" + p.id() + " " + switch (p.type()) {
            case HEAVY -> "Heavy";
            case NORMAL -> "Normal";
            case LIGHT -> "Light";
        };
        g.drawString(label, sx - g.getFontMetrics().stringWidth(label) / 2, sy + 34);
    }

    private void drawUI(Graphics2D g, PlayerView me) {
        g.setColor(new Color(0,0,0,175));
        g.fillRect(15, 15, 300, 137);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        if (me != null) {
            g.drawString("HP: " + Math.max(0, me.hp()) + "/" + me.maxHealth(), 30, 40);
            g.drawString("Ammo: " + me.ammo(), 30, 65);
            g.drawString("Loot: " + me.loot(), 30, 90);
            g.drawString("Players: " + players.size(), 30, 115);
            g.drawString("Weapon: " + me.weapon(), 30, 140);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.drawString(connectionStatus, 700, 30);
        g.drawString("WASD move | Mouse shoot", 700, 48);
        g.drawString("Ctrl sneak | Shift sprint | Space dash", 650, 84);
        g.drawString("Blue = rapid fire, red = damage boost", 680, 102);
        g.drawString("Loot every 5s: gold = ammo, cross = health", 700, 66);
        if (System.currentTimeMillis() < notificationUntil) {
            g.setColor(new Color(240, 190, 60));
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(notification, 30, 180);
        }

    }
}
