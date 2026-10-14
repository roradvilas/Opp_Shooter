import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Executable examples and checks for the five requested requirements. */
public final class DesignPatternsDemo {
    public static void main(String[] args) {
        AtomicLong ids = new AtomicLong();
        for (PlayerType type : PlayerType.values()) {
            Player player = PlayerFactory.forType(type).createPlayer(1, 200, 200);
            System.out.printf("PlayerFactory: %s -> %d HP, speed %.2f%n",
                    player.getClass().getSimpleName(), player.maxHealth, player.speed);
        }
        WeaponFactory weapons = new WeaponFactory();
        for (WeaponType type : WeaponType.values()) {
            WeaponStrategy weapon = weapons.createWeapon(type);
            check(weapon.fire(new NormalPlayer(1, 200, 200), 0, ids::incrementAndGet).size() == weapon.bulletCount(),
                    "Wrong projectile count");
            System.out.printf("WeaponFactory: %s -> %d ms, %d bullets%n",
                    weapon.getClass().getSimpleName(), weapon.shootingDelay(), weapon.bulletCount());
        }
        for (AbstractFactory factory : List.of(new CombatFactory(), new TrainingFactory())) {
            Player player = factory.createPlayer(factory.defaultPlayerType(), 1, 200, 200);
            WeaponStrategy weapon = factory.createWeapon(factory.defaultWeaponType());
            MovementStrategy movement = factory.createMovement();
            check(factory instanceof CombatFactory
                    ? player instanceof NormalPlayer && weapon instanceof AssaultRifle && movement instanceof Walk
                    : player instanceof LightPlayer && weapon instanceof Pistol && movement instanceof Sneak,
                    "Abstract Factory returned incorrect default family");
            System.out.printf("AbstractFactory: %s -> %s + %s + %s%n", factory.getClass().getSimpleName(),
                    player.getClass().getSimpleName(), weapon.getClass().getSimpleName(), movement.getClass().getSimpleName());
        }
        PlayerBuilder combat = new CombatPlayerBuilder();
        PlayerBuilder training = new TrainingPlayerBuilder();
        check(combat.build() instanceof NormalPlayer && combat.build().weaponType == WeaponType.ASSAULT_RIFLE
                && combat.build().getMovementType() == MovementType.WALK, "Combat recipe");
        check(training.build() instanceof LightPlayer && training.build().weaponType == WeaponType.PISTOL
                && training.build().getMovementType() == MovementType.SNEAK, "Training recipe");
        for (PlayerBuilder builder : List.of(combat, training)) {
            for (PlayerType type : PlayerType.values()) for (WeaponType weapon : WeaponType.values()) {
                builder.playerType(type).weaponType(weapon).identity(42).position(200, 200);
                Player first = builder.build(), second = builder.build();
                check(first != second && first.type == type && first.weaponType == weapon, "Builder choices");
                first.damage(10);
                first.shoot(ids::incrementAndGet, 300, 200, 1000);
                check(second.health == second.maxHealth && second.ammo == 60 && second.lastShot == 0,
                        "Builder products share gameplay state");
                System.out.printf("Builder: %s -> %s + %s%n", builder.getClass().getSimpleName(), type.name(), weapon.name());
            }
        }
        Player player = new NormalPlayer(42, 200, 200);
        World open = new WorldBuilder().obstacles(0).build();
        for (MovementType type : MovementType.values()) {
            player.setMovement(type);
            double x = player.x;
            player.move(false, false, false, true, open, 1000);
            System.out.printf("Strategy: %s -> %.2f units/tick%n", type.createStrategy().getClass().getSimpleName(), player.x - x);
        }
        List<PlayerEvent> events = new ArrayList<>(), audit = new ArrayList<>();
        PlayerObserver observer = audit::add;
        player.addObserver(events::add);
        player.addObserver(observer);
        player.damage(100);
        check(events.equals(List.of(new PlayerEvent(42, PlayerEvent.Type.DAMAGED, 100),
                new PlayerEvent(42, PlayerEvent.Type.DIED, 0))) && events.equals(audit), "Observer order");
        player.removeObserver(observer);
        player.health = player.maxHealth;
        player.collectLoot(LootType.AMMO);
        check(events.size() == 3 && audit.size() == 2, "Observer removal");
        System.out.println("Observer: both subscribers received DAMAGED -> DIED; removed subscriber received no later loot event");
        System.out.println("PASS: Factory, Abstract Factory, four movement Strategies, Observer, two concrete PlayerBuilders");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
