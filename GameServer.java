import java.awt.Point;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private final int port;
    private final World world = new World();
    private final Map<Integer, Client> clients = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();
    private int nextPlayerId = 1;
    private long nextBulletId = 1;

    public GameServer(int port) { this.port = port; }

    public void start() throws IOException {
        Logger.getInstance().log("PvP server listening on ws://0.0.0.0:" + port);
        try (ServerSocket server = new ServerSocket(port)) {
            new Thread(this::gameLoop, "game-loop").start();
            while (true) {
                Socket socket = server.accept();
                try {
                    WebSocketServer ws = new WebSocketServer(socket);
                    if (!ws.handshake()) { socket.close(); continue; }

                    int id = nextPlayerId++;
                    Client client = new Client(id, ws);
                    clients.put(id, client);
                    new Thread(client, "player-" + id).start();

                    Logger.getInstance().log("Player " + id + " connected.");
                } catch (Exception e) {
                    try { socket.close(); } catch (IOException ignored) {}
                    e.printStackTrace();
                }
            }
        }
    }

    private Player spawnPlayer(int id, PlayerType type) {
        PlayerFactory factory = PlayerFactory.forType(type);
        for (;;) {
            double x = 80 + random.nextInt(world.getWidth() - 160);
            double y = 80 + random.nextInt(world.getHeight() - 160);
            Player player = factory.createPlayer(id, x, y);
            if (!world.collidesWithWall(x, y, player.radius)) return player;
        }
    }

    private void gameLoop() {
        final long tick = 1000 / 60;
        long next = System.currentTimeMillis();

        while (true) {
            long now = System.currentTimeMillis();
            if (now >= next) {
                update(now);
                broadcastState();
                next += tick;
                if (now - next > 1000) next = now;
            } else {
                try { Thread.sleep(Math.min(2, next - now)); }
                catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(long now) {
        for (Client c : clients.values()) {
            Player p = players.get(c.id);
            if (p == null || p.health <= 0) continue;

            p.move(c.w, c.s, c.a, c.d, world);

            if (c.shoot && p.canShoot(now)) {
                Bullet b = p.shoot(nextBulletId++, c.mouseWorldX, c.mouseWorldY, now);
                if (b != null) bullets.add(b);
            }

            collectLoot(p);
        }

        synchronized (bullets) {
            for (Bullet b : bullets) {
                if (!b.alive) continue;
                b.update(world);
                if (!b.alive) continue;

                for (Player p : players.values()) {
                    if (p.id == b.ownerId || p.health <= 0) continue;
                    if (Math.hypot(p.x - b.x, p.y - b.y) < p.radius + 5) {
                        p.damage(b.damage);
                        b.alive = false;
                        if (p.health == 0) {
                            synchronized (world.getLoot()) { world.getLoot().add(new Point((int)p.x, (int)p.y)); }
                        }
                        break;
                    }
                }
            }
            bullets.removeIf(b -> !b.alive);
        }
    }

    private void collectLoot(Player p) {
        synchronized (world.getLoot()) {
            for (int i = world.getLoot().size() - 1; i >= 0; i--) {
                Point q = world.getLoot().get(i);
                if (Math.hypot(p.x - q.x, p.y - q.y) < 28) {
                    world.getLoot().remove(i);
                    p.loot++;
                    p.ammo += 5;
                }
            }
        }
    }

    private void broadcastState() {
        StringBuilder s = new StringBuilder("STATE|").append(nextPlayerId - 1).append("|");
        boolean first = true;
        for (Player p : players.values()) {
            if (!first) s.append(';');
            first = false;
            s.append(p.id).append(',').append(Math.round(p.x)).append(',')
             .append(Math.round(p.y)).append(',').append(p.health).append(',')
             .append(p.ammo).append(',').append(p.loot).append(',')
             .append(p.type.name()).append(',').append(p.maxHealth);
        }

        s.append("|");
        first = true;
        synchronized (bullets) {
            for (Bullet b : bullets) {
                if (!first) s.append(';');
                first = false;
                s.append(b.id).append(',').append(Math.round(b.x)).append(',')
                 .append(Math.round(b.y));
            }
        }

        s.append("|");
        first = true;
        synchronized (world.getLoot()) {
            for (Point p : world.getLoot()) {
                if (!first) s.append(';');
                first = false;
                s.append(p.x).append(',').append(p.y);
            }
        }

        String state = s.toString();
        for (Client c : clients.values()) {
            try { c.send(state); }
            catch (Exception e) { disconnect(c.id); }
        }
    }

    private void disconnect(int id) {
        Client c = clients.remove(id);
        players.remove(id);
        if (c != null) try { c.ws.close(); } catch (Exception ignored) {}
        Logger.getInstance().log("Player " + id + " disconnected.");
    }

    private class Client implements Runnable {
        final int id;
        final WebSocketServer ws;
        volatile boolean w, s, a, d, shoot;
        volatile double mouseWorldX, mouseWorldY;

        Client(int id, WebSocketServer ws) {
            this.id = id; this.ws = ws;
        }

        void send(String text) throws IOException { ws.sendText(text); }

        @Override public void run() {
            try {
                while (true) {
                    String msg = ws.readText();
                    if (msg == null) break;
                    if (!players.containsKey(id)) {
                        String[] join = msg.split("\\|", -1);
                        if (join.length != 2 || !join[0].equals("JOIN")) continue;
                        PlayerType type;
                        try { type = PlayerType.valueOf(join[1]); }
                        catch (IllegalArgumentException e) { continue; }
                        players.put(id, spawnPlayer(id, type));
                        send("WELCOME|" + id);
                    } else {
                        parseInput(msg);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                disconnect(id);
            }
        }

        void parseInput(String msg) {
            String[] p = msg.split("\\|");
            if (p.length != 8 || !p[0].equals("INPUT")) return;
            w = p[1].equals("1"); s = p[2].equals("1");
            a = p[3].equals("1"); d = p[4].equals("1");
            shoot = p[5].equals("1");
            try {
                mouseWorldX = Double.parseDouble(p[6]);
                mouseWorldY = Double.parseDouble(p[7]);
            } catch (NumberFormatException ignored) {}
        }
    }
}
