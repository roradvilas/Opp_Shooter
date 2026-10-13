import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class DesignPatternsTest {
    public static void main(String[] args) throws Exception {
        testBuilder();
        testObservers();
        testPrototype();
        testClientEventsAndMap();
        Logger.getInstance().log("PASS: builder isolation/reproducibility, observer events, bullet prototypes, client map/events");
    }

    private static void testBuilder() {
        WorldBuilder builder = new WorldBuilder().size(20, 15).seed(17).obstacles(12).lootAttempts(30);
        World first = builder.build(), second = builder.build();
        check(first.getWidth() == 800 && first.getHeight() == 600, "Map dimensions");
        check(first.getLoot().equals(second.getLoot()), "Seed must reproduce loot");
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 20; x++) {
                check(first.isWall(x * 40, y * 40) == second.isWall(x * 40, y * 40), "Seed must reproduce walls");
                if (x == 0 || y == 0 || x == 19 || y == 14)
                    check(first.isWall(x * 40, y * 40), "Border missing");
            }
        }
        for (Loot point : first.getLoot()) check(!first.isWall(point.x(), point.y()), "Loot inside wall");
        first.getLoot().clear();
        check(!second.getLoot().isEmpty(), "Builds share mutable loot");
        World empty = builder.obstacles(0).lootAttempts(0).build();
        check(empty.getLoot().isEmpty() && !empty.isWall(100, 100), "Builder configuration");
        try { builder.size(7, 8); throw new AssertionError("Invalid size accepted"); }
        catch (IllegalArgumentException expected) {}
    }

    private static void testObservers() {
        Player player = new Player(42, 100, 100);
        List<PlayerEvent> events = new ArrayList<>(), otherEvents = new ArrayList<>();
        PlayerObserver observer = events::add;
        PlayerObserver other = otherEvents::add;
        player.addObserver(observer);
        player.addObserver(other);
        player.collectLoot(LootType.AMMO);
        player.damage(30);
        player.damage(200);
        player.damage(1);
        player.damage(-10);
        check(events.equals(List.of(
                new PlayerEvent(42, PlayerEvent.Type.LOOT_COLLECTED, 5),
                new PlayerEvent(42, PlayerEvent.Type.DAMAGED, 30),
                new PlayerEvent(42, PlayerEvent.Type.DAMAGED, 70),
                new PlayerEvent(42, PlayerEvent.Type.DIED, 0))), "Wrong event sequence or repeated death");
        check(events.equals(otherEvents), "Observers did not receive same events");
        check(player.loot == 1 && player.ammo == 65 && player.health == 0, "Event state changes");
        player.removeObserver(other);
        player.health = player.maxHealth;
        player.collectLoot(LootType.AMMO);
        check(otherEvents.size() == 4 && events.size() == 5, "Observer removal");
        Player shooter = new Player(1, 100, 100);
        shooter.addObserver(observer);
        shooter.ammo = 1;
        shooter.shoot(() -> 1, 200, 100, 1000);
        check(events.get(5).type() == PlayerEvent.Type.AMMO_EMPTY, "Missing empty ammo event");
    }

    private static void testPrototype() {
        Bullet prototype = new Bullet(0, 0, 0, 0, 12, 0);
        prototype.damage = 40;
        prototype.alive = false;
        Bullet first = prototype.copy(10, 5, 100, 100, Math.PI / 2);
        Bullet second = prototype.copy(11, 6, 200, 200, 0);
        check(first != second && first != prototype, "Copy shares identity");
        check(first.id == 10 && first.ownerId == 5 && first.alive && first.damage == 40, "Fresh bullet state");
        check(Math.abs(first.vy - 12) < 1e-9 && Math.abs(first.vx) < 1e-9, "Prototype velocity");
        first.damage = 1;
        first.update(new WorldBuilder().obstacles(0).lootAttempts(0).build());
        check(second.x == 200 && second.y == 200 && second.damage == 40 && prototype.x == 0,
                "Bullet mutations affect other copies");
        WeaponStrategy weapon = new Pistol();
        weapon.bulletPrototype().damage = 33;
        Bullet shot = weapon.fire(new Player(7, 100, 100), 0, () -> 12).get(0);
        check(shot.damage == 33 && shot.id == 12 && shot.ownerId == 7, "Weapon did not clone its prototype");
    }

    private static void testClientEventsAndMap() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game game = new Game();
                game.onText("WELCOME|42|1234567");
                World client = (World)field(game, "world");
                World server = new WorldBuilder().seed(1234567).build();
                for (int y = 0; y < server.getHeight(); y += World.TILE)
                    for (int x = 0; x < server.getWidth(); x += World.TILE)
                        check(client.isWall(x, y) == server.isWall(x, y), "Client map differs from server");
                game.onText("EVENT|LOOT_COLLECTED|5");
                check(field(game, "notification").equals("Loot collected! +5 ammo"), "HUD event missing");
                check((long)field(game, "notificationUntil") > System.currentTimeMillis(), "Notification expired");
                game.onText("SPAWNED");
                check(field(game, "notification").equals(""), "Respawn kept old event");
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
