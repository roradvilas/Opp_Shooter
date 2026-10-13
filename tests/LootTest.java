import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class LootTest {
    public static void main(String[] args) throws Exception {
        testTimer();
        testSpawnLocations();
        testSpacingBoundary();
        testRewards();
        testHealthCollection();
        testRendering();
        Logger.getInstance().log("PASS: five-second loot timer, safe spawning, ammo/healing limits, pickup removal, typed loot rendering");
    }

    private static void testTimer() throws Exception {
        GameServer server = new GameServer(0);
        World world = (World)field(server, "world");
        Method update = GameServer.class.getDeclaredMethod("update", long.class);
        update.setAccessible(true);
        check(world.getLoot().isEmpty(), "Loot exists before timer starts");
        update.invoke(server, 1000L);
        update.invoke(server, 5999L);
        check(world.getLoot().isEmpty(), "Loot spawned before five seconds");
        update.invoke(server, 6000L);
        check(world.getLoot().size() == 1, "Missing first timed pickup");
        update.invoke(server, 6000L);
        update.invoke(server, 10999L);
        check(world.getLoot().size() == 1, "Loot spawned twice in one interval");
        update.invoke(server, 11000L);
        check(world.getLoot().size() == 2, "Missing second timed pickup");
        update.invoke(server, 100000L);
        check(world.getLoot().size() == 3, "Delayed tick caused a loot burst");
    }

    private static void testSpawnLocations() {
        World world = new WorldBuilder().size(8, 8).obstacles(0).build();
        Random random = new Random(23);
        Set<String> positions = new HashSet<>();
        Set<LootType> types = new HashSet<>();
        while (world.spawnLoot(random)) {
            check(world.getLoot().size() <= 36, "Spawning did not stop when space ran out");
            Loot pickup = world.getLoot().get(world.getLoot().size() - 1);
            check(!world.collidesWithWall(pickup.x(), pickup.y(), 16), "Pickup inside wall");
            check(positions.add(pickup.x() + "," + pickup.y()), "Overlapping pickups");
            types.add(pickup.type());
            assertSpacing(world.getLoot());
        }
        check(types.size() == 2, "Only one loot type spawned");
        int count = world.getLoot().size();
        check(count > 0 && count < 36, "Spacing did not limit spawn density");
        check(!world.spawnLoot(random) && world.getLoot().size() == count, "Map without spaced positions must skip spawn");
        world.getLoot().clear();
        check(world.spawnLoot(random), "Removing loot did not free spawn space");
        World built = new WorldBuilder().seed(23).lootAttempts(500).build();
        check(built.getLoot().size() > 1, "Builder failed to place loot");
        assertSpacing(built.getLoot());
    }

    private static void testSpacingBoundary() {
        for (LootType type : LootType.values()) {
            List<Loot> existing = List.of(new Loot(200, 200, type));
            check(!World.hasLootClearance(200, 200, existing), "Overlap allowed");
            check(!World.hasLootClearance(279, 200, existing), "Inside radius allowed");
            check(!World.hasLootClearance(280, 200, existing), "Exactly two tiles allowed");
            check(!World.hasLootClearance(200, 120, existing), "Vertical radius boundary allowed");
            check(!World.hasLootClearance(240, 240, existing), "Diagonal inside radius allowed");
            check(World.hasLootClearance(281, 200, existing), "Outside radius blocked");
            check(World.hasLootClearance(280, 240, existing), "Diagonal outside circular radius blocked");
        }
    }

    private static void assertSpacing(List<Loot> pickups) {
        for (int i = 0; i < pickups.size(); i++) {
            for (int j = 0; j < i; j++) {
                Loot a = pickups.get(i), b = pickups.get(j);
                check(Math.hypot(a.x() - b.x(), a.y() - b.y()) > 2 * World.TILE,
                        "Loot spawned within two-tile radius");
            }
        }
    }

    private static void testRewards() {
        for (PlayerType type : PlayerType.values()) {
            Player player = PlayerFactory.forType(type).createPlayer(1, 60, 60);
            List<PlayerEvent> events = new ArrayList<>();
            player.addObserver(events::add);
            check(!player.collectLoot(LootType.HEALTH) && player.loot == 0 && events.isEmpty(),
                    "Full-health player consumed health");
            player.health -= 10;
            check(player.collectLoot(LootType.HEALTH), "Health pickup rejected");
            check(player.health == player.maxHealth && player.ammo == 60 && player.loot == 1, "Healing cap/stats");
            check(events.get(0).equals(new PlayerEvent(1, PlayerEvent.Type.HEALTH_COLLECTED, 10)), "Actual healing event");
            player.health = 1;
            player.collectLoot(LootType.HEALTH);
            check(player.health == 26, "Health pickup amount");
            player.collectLoot(LootType.AMMO);
            check(player.ammo == 65 && player.health == 26 && player.loot == 3, "Ammo reward changed health");
            player.health = 0;
            check(!player.collectLoot(LootType.HEALTH) && !player.collectLoot(LootType.AMMO), "Dead player collected loot");
        }
    }

    private static void testHealthCollection() throws Exception {
        GameServer server = new GameServer(0);
        World world = (World)field(server, "world");
        Player player = new Player(1, 60, 60);
        Loot health = new Loot(60, 60, LootType.HEALTH);
        world.getLoot().add(health);
        Method collect = GameServer.class.getDeclaredMethod("collectLoot", Player.class);
        collect.setAccessible(true);
        collect.invoke(server, player);
        check(world.getLoot().contains(health), "Full-health pickup disappeared");
        player.health = 50;
        collect.invoke(server, player);
        check(player.health == 75 && world.getLoot().isEmpty(), "Health not applied/removed by server");
        collect.invoke(server, player);
        check(player.health == 75 && player.loot == 1, "Pickup applied twice");
    }

    private static void testRendering() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game game = new Game();
                game.setSize(960, 640);
                ((JPanel)field(game, "spawnScreen")).setVisible(false);
                game.onText("STATE|0|||400,400,AMMO;440,400,HEALTH");
                BufferedImage image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                game.paint(graphics);
                graphics.dispose();
                check(image.getRGB(400, 400) == new Color(240, 190, 60).getRGB(), "Ammo visual missing");
                check(image.getRGB(440, 400) == Color.WHITE.getRGB(), "Health cross missing");
                check(image.getRGB(434, 394) == new Color(65, 200, 100).getRGB(), "Health background missing");
                game.onText("EVENT|HEALTH_COLLECTED|10");
                check(field(game, "notification").equals("Health collected! +10 HP"), "Healing HUD event missing");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
