import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GameplayTest {
    public static void main(String[] args) throws Exception {
        testWeapons();
        testClientScreenAndLoot();
        testLifecycle();
        Logger.getInstance().log("PASS: weapon counts/rates/spread/ammo/IDs, client screens/loot, death/respawn/aim synchronization");
    }

    private static void testWeapons() {
        AtomicLong counter = new AtomicLong(1);
        Set<Long> ids = new HashSet<>();
        for (WeaponType type : WeaponType.values()) {
            WeaponStrategy strategy = type.createStrategy();
            int expectedCount = type == WeaponType.SHOTGUN ? 3 : 1;
            int expectedDelay = switch (type) {
                case ASSAULT_RIFLE -> 130;
                case PISTOL -> 325;
                case SHOTGUN -> 650;
            };
            check(strategy.shootingDelay() == expectedDelay, "Fire rate");
            for (int sample = 0; sample < 100; sample++) {
                Player player = new HeavyPlayer(1, 100, 100);
                player.setWeapon(type);
                List<Bullet> shot = player.shoot(counter::getAndIncrement, 200, 100, 1000);
                check(shot.size() == expectedCount, "Pellet count");
                check(player.ammo == 60 - expectedCount, "Shared ammo cost");
                for (Bullet bullet : shot) {
                    check(ids.add(bullet.id), "Duplicate bullet ID across weapons");
                    near(Math.hypot(bullet.vx, bullet.vy), 9);
                    check(bullet.ownerId == 1 && bullet.damage == 25, "Bullet stats");
                }
                if (type == WeaponType.SHOTGUN) {
                    double left = Math.toDegrees(Math.atan2(shot.get(0).vy, shot.get(0).vx));
                    double right = Math.toDegrees(Math.atan2(shot.get(2).vy, shot.get(2).vx));
                    check(left >= -15 && left <= -3 && right >= 3 && right <= 15, "Spread range");
                    near(shot.get(1).vy, 0);
                }
                check(!player.canShoot(1000 + expectedDelay - 1), "Cooldown too short");
                check(player.canShoot(1000 + expectedDelay), "Cooldown too long");
                long previous = counter.get();
                check(player.shoot(counter::getAndIncrement, 200, 100, 1001).isEmpty(), "Cooldown ignored");
                check(counter.get() == previous, "Rejected shot used bullet IDs");
                player.ammo = expectedCount - 1;
                check(player.shoot(counter::getAndIncrement, 200, 100, 2000).isEmpty(), "Insufficient ammo");
                player.ammo = 60;
                check(player.shoot(counter::getAndIncrement, 100, 100, 2000).isEmpty(), "Zero aim");
                player.health = 0;
                check(!player.canShoot(2000), "Dead player can shoot");
            }
        }
    }

    private static void testClientScreenAndLoot() throws Exception {
        Game[] client = new Game[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game game = new Game();
                client[0] = game;
                game.setSize(Game.SCREEN_WIDTH, Game.SCREEN_HEIGHT);
                JPanel screen = (JPanel)field(game, "spawnScreen");
                JButton button = (JButton)field(game, "spawnButton");
                check(screen.isVisible() && !button.isEnabled(), "Initial selection screen");
                check(field(game, "classSelection") != null && field(game, "weaponSelection") != null, "Loadout selectors");
                screen.setVisible(false);
                game.onText("STATE|0|||400,400");
                BufferedImage image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
                game.paint(image.getGraphics());
                int gold = new Color(240, 190, 60).getRGB();
                check(image.getRGB(400, 400) == gold, "Authoritative loot missing");
                game.onText("STATE|0|||");
                game.paint(image.getGraphics());
                check(image.getRGB(400, 400) != gold, "Picked-up loot still visible");
                World world = (World)field(game, "world");
                Point originalLoot = world.getLoot().get(0);
                BufferedImage map = new BufferedImage(world.getWidth(), world.getHeight(), BufferedImage.TYPE_INT_RGB);
                world.draw(map.createGraphics(), 0, 0, world.getWidth(), world.getHeight());
                check(map.getRGB(originalLoot.x, originalLoot.y) != gold, "Local map duplicates server loot");
                game.onText("WELCOME|1");
                game.onText("STATE|1|1,480,320,100,60,0,HEAVY,100,0,PISTOL||");
                Input input = (Input)field(game, "input");
                input.mouseMoved(new MouseEvent(game, MouseEvent.MOUSE_MOVED, 0, 0, 480, 420, 0, false));
                game.paint(image.getGraphics());
                check(image.getRGB(480, 340) == Color.DARK_GRAY.getRGB(), "Local weapon does not follow mouse");
                game.onText("STATE|1|2,480,320,80,60,0,LIGHT,80,1.5707963267948966,SHOTGUN||");
                game.paint(image.getGraphics());
                check(image.getRGB(480, 340) == Color.DARK_GRAY.getRGB(), "Remote weapon does not follow synchronized angle");
                game.onText("DIED");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel screen = (JPanel)field(client[0], "spawnScreen");
                JButton button = (JButton)field(client[0], "spawnButton");
                check(screen.isVisible() && button.getText().equals("Respawn"), "Death screen");
                client[0].onText("SPAWNED");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        SwingUtilities.invokeAndWait(() -> {
            try { check(!((JPanel)field(client[0], "spawnScreen")).isVisible(), "Spawn screen did not close"); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @SuppressWarnings("unchecked")
    private static void testLifecycle() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        GameServer server = new GameServer(port);
        Thread thread = new Thread(() -> {
            try { server.start(); } catch (Exception e) { throw new RuntimeException(e); }
        });
        thread.setDaemon(true);
        thread.start();
        Messages first = new Messages(), second = new Messages();
        WebSocketClient observer = connect(port, first), victim = null;
        try {
            victim = connect(port, second);
            int observerId = Integer.parseInt(first.await("WELCOME|").split("\\|")[1]);
            int victimId = Integer.parseInt(second.await("WELCOME|").split("\\|")[1]);
            check(first.await("STATE|").split("\\|", -1)[2].isEmpty(), "Connection spawned without selection");
            observer.sendText("SPAWN|HEAVY|PISTOL");
            victim.sendText("SPAWN|LIGHT|SHOTGUN");
            first.await("SPAWNED");
            second.await("SPAWNED");
            Map<Integer, Player> players = (Map<Integer, Player>)field(server, "players");
            World world = (World)field(server, "world");
            List<Bullet> bullets = (List<Bullet>)field(server, "bullets");
            synchronized (server) {
                Player shooter = players.get(observerId), target = players.get(victimId);
                check(shooter.weaponType == WeaponType.PISTOL && target.weaponType == WeaponType.SHOTGUN, "Selected weapon");
                shooter.x = 60; shooter.y = 60;
                target.x = 140; target.y = 60;
                target.health = 25;
                synchronized (world.getLoot()) {
                    world.getLoot().clear();
                    world.getLoot().add(new Point(60, 60));
                }
                bullets.add(new Bullet(9000, observerId, 140, 60, 0, 0));
                Method update = GameServer.class.getDeclaredMethod("update", long.class);
                update.setAccessible(true);
                update.invoke(server, System.currentTimeMillis());
                check(!players.containsKey(victimId), "Dead body remains on server");
                check(shooter.loot == 1 && shooter.ammo == 65, "Pickup reward");
                check(!world.getLoot().contains(new Point(60, 60)), "Picked-up loot remains");
            }
            second.await("DIED");
            first.queue.clear();
            String state = first.await("STATE|");
            check(!hasPlayer(state, victimId), "Dead body broadcast to other client");
            victim.sendText("SPAWN|HEAVY|ASSAULT_RIFLE");
            second.await("SPAWNED");
            synchronized (server) {
                Player respawned = players.get(victimId);
                check(respawned instanceof HeavyPlayer && respawned.health == 100
                        && respawned.ammo == 60 && respawned.loot == 0
                        && respawned.weaponType == WeaponType.ASSAULT_RIFLE, "Respawn loadout/reset");
                check(!world.collidesWithWall(respawned.x, respawned.y, respawned.radius), "Respawn inside wall");
            }
            observer.sendText("INPUT|0|0|0|0|0|60|160");
            first.queue.clear();
            boolean aimed = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!aimed && System.nanoTime() < deadline) {
                for (String row : first.await("STATE|").split("\\|", -1)[2].split(";")) {
                    String[] values = row.split(",");
                    if (values.length >= 10 && Integer.parseInt(values[0]) == observerId)
                        aimed = Math.abs(Double.parseDouble(values[8]) - Math.PI / 2) < 1e-9;
                }
            }
            check(aimed, "Aim rotation not synchronized");
            victim.sendText("SPAWN|LIGHT|PISTOL");
            Thread.sleep(50);
            synchronized (server) {
                check(players.get(victimId).type == PlayerType.HEAVY, "Alive player changed loadout");
            }
        } finally {
            observer.close();
            if (victim != null) victim.close();
        }
    }

    private static boolean hasPlayer(String state, int id) {
        for (String row : state.split("\\|", -1)[2].split(";"))
            if (row.startsWith(id + ",")) return true;
        return false;
    }

    private static WebSocketClient connect(int port, Messages messages) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            WebSocketClient client = new WebSocketClient("localhost", port, messages);
            try { client.connect(); return client; }
            catch (Exception e) { client.close(); Thread.sleep(20); }
        }
        throw new AssertionError("Server did not start");
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static class Messages implements WebSocketClient.Listener {
        final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        public void onText(String text) { queue.add(text); }
        public void onClose() {}
        String await(String prefix) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                String text = queue.poll(100, TimeUnit.MILLISECONDS);
                if (text != null && text.startsWith(prefix)) return text;
            }
            throw new AssertionError("Timeout waiting for " + prefix);
        }
    }

    private static void near(double value, double expected) { check(Math.abs(value - expected) < 1e-9, "Value mismatch"); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
