import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private final int port;
    private final World world = new WorldBuilder().build();
    private final Map<Integer, Client> clients = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();
    private int nextPlayerId = 1;
    private long nextBulletId = 1;
    private static final long LOOT_SPAWN_INTERVAL = 5000;
    private long nextLootSpawnAt = -1;

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
                    client.send("WELCOME|" + id + "|" + world.getSeed());
                    new Thread(client, "player-" + id).start();

                    Logger.getInstance().log("Player " + id + " connected.");
                } catch (Exception e) {
                    try { socket.close(); } catch (IOException ignored) {}
                    e.printStackTrace();
                }
            }
        }
    }

    private Player spawnPlayer(int id, PlayerType type, WeaponType weapon) {
        PlayerBuilder builder = new CombatPlayerBuilder().identity(id).playerType(type).weaponType(weapon);
        for (;;) {
            double x = 80 + random.nextInt(world.getWidth() - 160);
            double y = 80 + random.nextInt(world.getHeight() - 160);
            Player player = builder.position(x, y).build();
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

    private synchronized void update(long now) {
        if (nextLootSpawnAt < 0) nextLootSpawnAt = now + LOOT_SPAWN_INTERVAL;
        if (now >= nextLootSpawnAt) {
            world.spawnLoot(random);
            nextLootSpawnAt = now + LOOT_SPAWN_INTERVAL;
        }
        for (Client c : clients.values()) {
            Player p = players.get(c.id);
            if (p == null || p.health <= 0) continue;

            p.setMovement(c.movementType);
            p.move(c.w, c.s, c.a, c.d, world, now);
            p.aim(c.mouseWorldX, c.mouseWorldY);

            if (c.shoot && p.canShoot(now)) {
                bullets.addAll(p.shoot(() -> nextBulletId++, c.mouseWorldX, c.mouseWorldY, now));
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
                            players.remove(p.id);
                            Client victim = clients.get(p.id);
                            if (victim != null) {
                                victim.w = victim.s = victim.a = victim.d = victim.shoot = false;
                                try { victim.send("DIED"); }
                                catch (IOException e) { disconnect(p.id); }
                            }
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
                Loot q = world.getLoot().get(i);
                if (Math.hypot(p.x - q.x(), p.y - q.y()) < 28 && p.collectLoot(q.type())) {
                    world.getLoot().remove(i);
                }
            }
        }
    }

    private synchronized void broadcastState() {
        StringBuilder s = new StringBuilder("STATE|").append(nextPlayerId - 1).append("|");
        boolean first = true;
        for (Player p : players.values()) {
            if (!first) s.append(';');
            first = false;
            s.append(p.id).append(',').append(Math.round(p.x)).append(',')
             .append(Math.round(p.y)).append(',').append(p.health).append(',')
             .append(p.ammo).append(',').append(p.loot).append(',')
             .append(p.type.name()).append(',').append(p.maxHealth).append(',')
             .append(p.angle).append(',').append(p.weaponType.name());
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
            for (Loot p : world.getLoot()) {
                if (!first) s.append(';');
                first = false;
                s.append(p.x()).append(',').append(p.y()).append(',').append(p.type().name());
            }
        }

        String state = s.toString();
        for (Client c : clients.values()) {
            try { c.send(state); }
            catch (Exception e) { disconnect(c.id); }
        }
    }

    private synchronized void disconnect(int id) {
        Client c = clients.remove(id);
        players.remove(id);
        if (c != null) try { c.ws.close(); } catch (Exception ignored) {}
        Logger.getInstance().log("Player " + id + " disconnected.");
    }

    private synchronized void handleMessage(Client client, String msg) throws IOException {
        if (!clients.containsKey(client.id)) return;
        String[] parts = msg.split("\\|", -1);
        if (parts[0].equals("SPAWN") || parts[0].equals("JOIN")) {
            if (players.containsKey(client.id) || (parts.length != 2 && parts.length != 3)) return;
            PlayerType type;
            WeaponType weapon;
            try {
                type = PlayerType.valueOf(parts[1]);
                weapon = parts.length == 3 ? WeaponType.valueOf(parts[2]) : WeaponType.ASSAULT_RIFLE;
            } catch (IllegalArgumentException e) { return; }
            Player player = spawnPlayer(client.id, type, weapon);
            player.addObserver(client);
            client.w = client.s = client.a = client.d = client.shoot = false;
            client.movementType = MovementType.WALK;
            client.mouseWorldX = player.x + 100;
            client.mouseWorldY = player.y;
            players.put(client.id, player);
            client.send("SPAWNED");
        } else if (players.containsKey(client.id)) {
            client.parseInput(msg);
        }
    }

    private class Client implements Runnable, PlayerObserver {
        final int id;
        final WebSocketServer ws;
        volatile boolean w, s, a, d, shoot;
        volatile double mouseWorldX, mouseWorldY;
        volatile MovementType movementType = MovementType.WALK;

        Client(int id, WebSocketServer ws) {
            this.id = id; this.ws = ws;
        }

        void send(String text) throws IOException { ws.sendText(text); }

        @Override public void onPlayerEvent(PlayerEvent event) {
            try { send("EVENT|" + event.type().name() + "|" + event.amount()); }
            catch (IOException e) { disconnect(id); }
        }

        @Override public void run() {
            try {
                while (true) {
                    String msg = ws.readText();
                    if (msg == null) break;
                    handleMessage(this, msg);
                }
            } catch (Exception ignored) {
            } finally {
                disconnect(id);
            }
        }

        void parseInput(String msg) {
            String[] p = msg.split("\\|");
            if ((p.length != 8 && p.length != 9) || !p[0].equals("INPUT")) return;
            try {
                double x = Double.parseDouble(p[6]), y = Double.parseDouble(p[7]);
                MovementType mode = p.length == 9 ? MovementType.valueOf(p[8]) : MovementType.WALK;
                if (Double.isFinite(x) && Double.isFinite(y)) {
                    w = p[1].equals("1"); s = p[2].equals("1");
                    a = p[3].equals("1"); d = p[4].equals("1");
                    shoot = p[5].equals("1");
                    mouseWorldX = x;
                    mouseWorldY = y;
                    movementType = mode;
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
