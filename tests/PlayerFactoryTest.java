import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PlayerFactoryTest {
    public static void main(String[] args) throws Exception {
        testStatsAndMovement();
        testCollisions();
        testServerSelection();
        Logger.getInstance().log("PASS: factory stats, movement, collisions, and WebSocket player selection");
    }

    private static void testStatsAndMovement() {
        World open = new World() {
            @Override public boolean collidesWithWall(double x, double y, int radius) { return false; }
        };
        for (PlayerType type : PlayerType.values()) {
            Player player = PlayerFactory.forType(type).createPlayer(1, 100, 100);
            check(switch (type) {
                case HEAVY -> player instanceof HeavyPlayer;
                case LIGHT -> player instanceof LightPlayer;
                case NORMAL -> player instanceof NormalPlayer;
            },
                    "Factory returned wrong class");
            check(player.health == switch (type) {
                case HEAVY -> 120;
                case LIGHT -> 80;
                case NORMAL -> 100;
            }, "Health");
            near(player.speed, switch (type) {
                case HEAVY -> 2.56;
                case LIGHT -> 3.84;
                case NORMAL -> 3.2;
            });
            check(player.ammo == 60 && player.radius == 16 && player.weaponType == WeaponType.ASSAULT_RIFLE, "Shared stats");
            player.move(false, false, false, true, open);
            near(player.x - 100, player.speed);
            player.x = player.y = 100;
            player.move(true, false, false, true, open);
            near(Math.hypot(player.x - 100, player.y - 100), player.speed);
            double x = player.x, y = player.y;
            player.move(true, true, true, true, open);
            near(player.x, x);
            near(player.y, y);
        }
    }

    private static void testCollisions() throws Exception {
        World world = new World();
        check(world.isWall(-1, 60), "Negative coordinates must be outside map");
        Random random = new Random(17);
        for (PlayerType type : PlayerType.values()) {
            for (int run = 0; run < 100; run++) {
                Player player;
                do {
                    player = PlayerFactory.forType(type).createPlayer(1,
                            40 + random.nextDouble() * (world.getWidth() - 80),
                            40 + random.nextDouble() * (world.getHeight() - 80));
                } while (world.collidesWithWall(player.x, player.y, player.radius));
                for (int tick = 0; tick < 1000; tick++) {
                    double x = player.x, y = player.y;
                    player.move(random.nextBoolean(), random.nextBoolean(),
                            random.nextBoolean(), random.nextBoolean(), world);
                    check(!world.collidesWithWall(player.x, player.y, player.radius), "Wall penetration");
                    check(Math.hypot(player.x - x, player.y - y) <= player.speed + 1e-9, "Excess movement");
                }
            }
        }
        // A vertical wall exercises sustained pressure, sliding, and faster movement.
        Field field = World.class.getDeclaredField("walls");
        field.setAccessible(true);
        boolean[][] walls = (boolean[][])field.get(world);
        for (int y = 0; y < walls.length; y++) {
            for (int x = 0; x < walls[y].length; x++) walls[y][x] = x == 5;
        }
        Player light = new LightPlayer(1, 180, 100);
        for (int i = 0; i < 100; i++) light.move(false, true, false, true, world);
        check(light.x + light.radius < 200, "Light passed through wall");
        check(light.y > 300, "Light should slide along wall");
    }

    private static void testServerSelection() throws Exception {
        int port;
        try (ServerSocket available = new ServerSocket(0)) { port = available.getLocalPort(); }
        Thread server = new Thread(() -> {
            try { new GameServer(port).start(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        server.setDaemon(true);
        server.start();
        WebSocketClient heavy = null, light = null, normal = null;
        try {
            Messages heavyMessages = new Messages();
            for (int attempt = 0; attempt < 100; attempt++) {
                try {
                    heavy = new WebSocketClient("localhost", port, heavyMessages);
                    heavy.connect();
                    break;
                } catch (Exception e) {
                    heavy.close();
                    heavy = null;
                    Thread.sleep(20);
                }
            }
            check(heavy != null, "Server did not start");
            heavy.sendText("JOIN|INVALID");
            heavy.sendText("JOIN|HEAVY");
            String welcome = heavyMessages.await("WELCOME|");
            int heavyId = Integer.parseInt(welcome.split("\\|")[1]);
            Messages lightMessages = new Messages();
            light = new WebSocketClient("localhost", port, lightMessages);
            light.connect();
            light.sendText("JOIN|LIGHT");
            int lightId = Integer.parseInt(lightMessages.await("WELCOME|").split("\\|")[1]);
            light.sendText("JOIN|HEAVY"); // Selection cannot change after spawning.
            boolean matched = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!matched && System.nanoTime() < deadline) {
                String state = lightMessages.await("STATE|");
                boolean foundHeavy = false, foundLight = false;
                for (String row : state.split("\\|", -1)[2].split(";")) {
                    String[] values = row.split(",");
                    if (values.length < 8) continue;
                    int id = Integer.parseInt(values[0]);
                    if (id == heavyId) foundHeavy = values[3].equals("120") && values[6].equals("HEAVY") && values[7].equals("120");
                    if (id == lightId) foundLight = values[3].equals("80") && values[6].equals("LIGHT") && values[7].equals("80");
                }
                matched = foundHeavy && foundLight;
            }
            check(matched, "Server did not broadcast both selected types");
            Messages normalMessages = new Messages();
            normal = new WebSocketClient("localhost", port, normalMessages);
            normal.connect();
            normal.sendText("SPAWN|NORMAL|SHOTGUN");
            int normalId = Integer.parseInt(normalMessages.await("WELCOME|").split("\\|")[1]);
            normalMessages.await("SPAWNED");
            boolean foundNormal = false;
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!foundNormal && System.nanoTime() < deadline) {
                for (String row : normalMessages.await("STATE|").split("\\|", -1)[2].split(";")) {
                    String[] values = row.split(",");
                    if (values.length >= 10 && Integer.parseInt(values[0]) == normalId)
                        foundNormal = values[3].equals("100") && values[6].equals("NORMAL")
                                && values[7].equals("100") && values[9].equals("SHOTGUN");
                }
            }
            check(foundNormal, "Normal player loadout missing from network state");
        } finally {
            if (heavy != null) heavy.close();
            if (light != null) light.close();
            if (normal != null) normal.close();
        }
    }

    private static class Messages implements WebSocketClient.Listener {
        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        public void onText(String text) { queue.add(text); }
        public void onClose() {}
        String await(String prefix) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                String text = queue.poll(100, TimeUnit.MILLISECONDS);
                if (text != null && text.startsWith(prefix)) return text;
            }
            throw new AssertionError("Timed out waiting for " + prefix);
        }
    }

    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-9, actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
