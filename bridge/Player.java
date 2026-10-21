package bridge;

import decorator.DamageBoostDecorator;
import decorator.RapidFireDecorator;
import decorator.WeaponDecorator;
import game.LootType;
import game.World;
import observer.PlayerEvent;
import observer.PlayerObserver;
import observer.PlayerSubject;
import prototype.Bullet;
import strategy.MovementStrategy;
import strategy.MovementType;
import strategy.Walk;
import strategy.WeaponStrategy;
import strategy.WeaponType;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

public class Player implements PlayerSubject {
    public final int id;
    public double x, y;
    public int radius = 16;
    public final PlayerType type;
    public final int maxHealth;
    public final double speed;
    public long dashUntil, nextDashAt;
    private MovementType movementType = MovementType.WALK;
    private MovementStrategy movement = new Walk();
    public int health, ammo = 60, loot = 0;
    public long lastShot = 0;
    public double angle;
    public WeaponType weaponType = WeaponType.ASSAULT_RIFLE;
    private WeaponStrategy weapon = weaponType.createStrategy();
    private Armor armor = new NoArmor();
    private final List<PlayerObserver> observers = new CopyOnWriteArrayList<>();

    @Override public void addObserver(PlayerObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    @Override public void removeObserver(PlayerObserver observer) { observers.remove(observer); }

    @Override public void notifyObservers(PlayerEvent event) {
        for (PlayerObserver observer : observers) observer.onPlayerEvent(event);
    }

    private void publish(PlayerEvent.Type type, int amount) {
        notifyObservers(new PlayerEvent(id, type, amount));
    }

    public Armor getArmor() { return armor; }
    public void setArmor(Armor armor) { this.armor = Objects.requireNonNull(armor); }

    public Player(int id, double x, double y) {
        this(id, x, y, PlayerType.NORMAL, 100, 3.2);
    }

    protected Player(int id, double x, double y, PlayerType type, int maxHealth, double speed) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
    }

    public void move(boolean w, boolean s, boolean a, boolean d, World world) {
        move(w, s, a, d, world, System.currentTimeMillis());
    }

    public void move(boolean w, boolean s, boolean a, boolean d, World world, long now) {
        double dx = (d ? 1 : 0) - (a ? 1 : 0);
        double dy = (s ? 1 : 0) - (w ? 1 : 0);
        if (dx == 0 && dy == 0) return;

        double movementSpeed = speed * movement.speedMultiplier(this, now, true);

        double len = Math.sqrt(dx * dx + dy * dy);
        dx = dx / len * movementSpeed;
        dy = dy / len * movementSpeed;

        // Small steps prevent faster players from skipping walls and allow wall sliding.
        int steps = (int)Math.ceil(movementSpeed);
        for (int i = 0; i < steps; i++) {
            if (!world.collidesWithWall(x + dx / steps, y, radius)) x += dx / steps;
            if (!world.collidesWithWall(x, y + dy / steps, radius)) y += dy / steps;
        }
    }

    public MovementType getMovementType() { return movementType; }
    public MovementStrategy getMovement() { return movement; }
    public WeaponStrategy getWeapon() { return weapon; }
    public void setMovement(MovementType type) {
        Objects.requireNonNull(type);
        if (movementType != type) equipMovement(type, type.createStrategy());
    }

    public void equipMovement(MovementType type, MovementStrategy strategy) {
        movementType = Objects.requireNonNull(type);
        movement = Objects.requireNonNull(strategy);
    }

    public boolean canShoot(long now) {
        return ammo >= weapon.bulletCount() && now - lastShot >= weapon.shootingDelay() && health > 0;
    }

    public void setWeapon(WeaponType type) {
        equipWeapon(type, Objects.requireNonNull(type).createStrategy());
    }

    public void equipWeapon(WeaponType type, WeaponStrategy strategy) {
        weaponType = Objects.requireNonNull(type);
        weapon = Objects.requireNonNull(strategy);
    }

    public void aim(double targetX, double targetY) {
        if (targetX != x || targetY != y) angle = Math.atan2(targetY - y, targetX - x);
    }

    public List<Bullet> shoot(LongSupplier bulletIds, double targetX, double targetY, long now) {
        double dx = targetX - x, dy = targetY - y;
        if (!canShoot(now) || (dx == 0 && dy == 0)) return List.of();
        aim(targetX, targetY);
        List<Bullet> shot = weapon.fire(this, angle, bulletIds);
        lastShot = now;
        ammo -= shot.size();
        if (ammo == 0) publish(PlayerEvent.Type.AMMO_EMPTY, 0);
        return shot;
    }

    public boolean collectLoot(LootType type) {
        Objects.requireNonNull(type);
        if (health <= 0 || (type == LootType.HEALTH && health == maxHealth)) return false;
        // Power-ups do not stack: one already worn is left on the ground.
        if (type == LootType.RAPID_FIRE && WeaponDecorator.isApplied(weapon, RapidFireDecorator.class)) return false;
        if (type == LootType.DAMAGE_BOOST && WeaponDecorator.isApplied(weapon, DamageBoostDecorator.class)) return false;
        loot++;
        if (type == LootType.AMMO) {
            ammo += type.amount();
            publish(PlayerEvent.Type.LOOT_COLLECTED, type.amount());
        } else if (type == LootType.HEALTH) {
            int restored = Math.min(type.amount(), maxHealth - health);
            health += restored;
            publish(PlayerEvent.Type.HEALTH_COLLECTED, restored);
        } else if (type == LootType.RAPID_FIRE) {
            equipWeapon(weaponType, new RapidFireDecorator(weapon));
            publish(PlayerEvent.Type.POWERUP, 0);
        } else {
            equipWeapon(weaponType, new DamageBoostDecorator(weapon));
            publish(PlayerEvent.Type.POWERUP, 0);
        }
        return true;
    }

    public void damage(int amount) {
        if (amount <= 0 || health <= 0) return;
        amount = armor.absorb(amount);
        int previous = health;
        health = Math.max(0, health - amount);
        publish(PlayerEvent.Type.DAMAGED, previous - health);
        if (health == 0) publish(PlayerEvent.Type.DIED, 0);
    }
}
