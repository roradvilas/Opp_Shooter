package facade;

import abstractfactory.AbstractFactory;
import bridge.Player;
import bridge.PlayerType;
import builder.PlayerBuilder;
import command.CommandInvoker;
import command.MoveCommand;
import command.ShootCommand;
import command.SwitchMovementCommand;
import command.SwitchWeaponCommand;
import game.Loot;
import game.World;
import game.WorldBuilder;
import prototype.Bullet;
import singleton.Logger;
import strategy.WeaponType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade over the game simulation. The server talks only to these few methods and never touches
 * the world, bullets, loot, factories, builders or per-player command history directly.
 * Callers must not use it from several threads at once without their own lock (GameServer serializes access).
 */
public final class GameFacade {
    private static final long LOOT_SPAWN_INTERVAL = 5000;

    private final AbstractFactory factory;
    private final PlayerBuilder builder;
    private final World world = new WorldBuilder().build();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final Map<Integer, CommandInvoker> invokers = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();
    private long nextBulletId = 1;
    private long nextLootSpawnAt = -1;
    private int lastSpawnedId;

    public GameFacade(AbstractFactory factory, PlayerBuilder builder) {
        this.factory = Objects.requireNonNull(factory);
        this.builder = Objects.requireNonNull(builder);
    }

    public long worldSeed() { return world.getSeed(); }

    public synchronized boolean hasPlayer(int id) { return players.containsKey(id); }

    /** Creates a player of the given class at a free random position and equips it through the builder. */
    public synchronized Player spawn(int id, PlayerType type, WeaponType weapon) {
        Player candidate;
        for (;;) {
            double x = 80 + random.nextInt(world.getWidth() - 160);
            double y = 80 + random.nextInt(world.getHeight() - 160);
            candidate = factory.create(type, id, x, y);
            if (!world.collidesWithWall(x, y, candidate.radius)) break;
        }
        Player player = builder.start(candidate).addWeapon(weapon).addArmor().addMovement().addAmmo().build();
        players.put(id, player);
        invokers.put(id, new CommandInvoker());
        lastSpawnedId = Math.max(lastSpawnedId, id);
        Logger.getInstance().log("Player " + id + " spawned as " + player.getClass().getSimpleName()
                + " with " + player.weaponType);
        return player;
    }

    /** Runs one tick of a player's held input: switches (only on change), movement, aiming, shooting, pickups. */
    public synchronized void applyInput(int id, PlayerInput input, long now) {
        Player p = players.get(id);
        CommandInvoker invoker = invokers.get(id);
        if (p == null || p.health <= 0) return;

        if (input.movement() != p.getMovementType()) invoker.run(new SwitchMovementCommand(p, input.movement()));
        if (input.weapon() != null && input.weapon() != p.weaponType)
            invoker.run(new SwitchWeaponCommand(p, input.weapon()));
        if (input.w() || input.s() || input.a() || input.d())
            invoker.run(new MoveCommand(p, input.w(), input.s(), input.a(), input.d(), world, now));
        p.aim(input.mouseX(), input.mouseY());
        if (input.shoot() && p.canShoot(now))
            invoker.run(new ShootCommand(p, () -> nextBulletId++, input.mouseX(), input.mouseY(), now, bullets));
        collectLoot(p);
    }

    /** Spawns timed loot and advances bullets. Returns the ids of players killed during this tick. */
    public synchronized List<Integer> tick(long now) {
        if (nextLootSpawnAt < 0) nextLootSpawnAt = now + LOOT_SPAWN_INTERVAL;
        if (now >= nextLootSpawnAt) {
            world.spawnLoot(random);
            nextLootSpawnAt = now + LOOT_SPAWN_INTERVAL;
        }

        List<Integer> killed = new ArrayList<>();
        synchronized (bullets) {
            for (Bullet b : bullets) {
                if (!b.alive) continue;
                b.update(world);
                if (!b.alive) continue;

                for (Player p : players.values()) {
                    if (p.id == b.ownerId || p.health <= 0) continue;
                    if (Math.hypot(p.x - b.x, p.y - b.y) < p.radius + 5) {
                        p.damage(b.effectiveDamage());
                        b.alive = false;
                        if (p.health == 0) {
                            remove(p.id);
                            killed.add(p.id);
                            Logger.getInstance().log("Player " + p.id + " was killed by player " + b.ownerId);
                        }
                        break;
                    }
                }
            }
            bullets.removeIf(b -> !b.alive);
        }
        return killed;
    }

    /** Removes a player (disconnect or death). Unknown ids are ignored. */
    public synchronized void remove(int id) {
        players.remove(id);
        invokers.remove(id);
    }

    /** A snapshot of the players currently in the match, for callers that need positions (bots). */
    public synchronized List<Player> players() { return new ArrayList<>(players.values()); }

    /** The STATE protocol message describing every player, bullet and pickup. */
    public synchronized String stateMessage() {
        List<Bullet> bulletSnapshot;
        synchronized (bullets) { bulletSnapshot = new ArrayList<>(bullets); }
        List<Loot> lootSnapshot;
        synchronized (world.getLoot()) { lootSnapshot = new ArrayList<>(world.getLoot()); }
        return Protocol.state(lastSpawnedId, players.values(), bulletSnapshot, lootSnapshot);
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
}
