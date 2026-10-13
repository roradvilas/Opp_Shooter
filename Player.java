import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

public class Player {
    final int id;
    double x, y;
    int radius = 16;
    final PlayerType type;
    final int maxHealth;
    final double speed;
    int health, ammo = 60, loot = 0;
    long lastShot = 0;
    double angle;
    WeaponType weaponType = WeaponType.ASSAULT_RIFLE;
    private WeaponStrategy weapon = weaponType.createStrategy();
    private final List<PlayerObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(PlayerObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    public void removeObserver(PlayerObserver observer) { observers.remove(observer); }

    private void notifyObservers(PlayerEvent.Type type, int amount) {
        PlayerEvent event = new PlayerEvent(id, type, amount);
        for (PlayerObserver observer : observers) observer.onPlayerEvent(event);
    }

    public Player(int id, double x, double y) {
        this(id, x, y, PlayerType.HEAVY, 100, 3.2);
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
        double dx = (d ? 1 : 0) - (a ? 1 : 0);
        double dy = (s ? 1 : 0) - (w ? 1 : 0);
        if (dx == 0 && dy == 0) return;

        double len = Math.sqrt(dx * dx + dy * dy);
        dx = dx / len * speed;
        dy = dy / len * speed;

        // Small steps prevent faster players from skipping walls and allow wall sliding.
        int steps = (int)Math.ceil(speed);
        for (int i = 0; i < steps; i++) {
            if (!world.collidesWithWall(x + dx / steps, y, radius)) x += dx / steps;
            if (!world.collidesWithWall(x, y + dy / steps, radius)) y += dy / steps;
        }
    }

    public boolean canShoot(long now) {
        return ammo >= weapon.bulletCount() && now - lastShot >= weapon.shootingDelay() && health > 0;
    }

    public void setWeapon(WeaponType type) {
        weaponType = type;
        weapon = type.createStrategy();
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
        if (ammo == 0) notifyObservers(PlayerEvent.Type.AMMO_EMPTY, 0);
        return shot;
    }

    public boolean collectLoot(LootType type) {
        Objects.requireNonNull(type);
        if (health <= 0 || (type == LootType.HEALTH && health == maxHealth)) return false;
        loot++;
        if (type == LootType.AMMO) {
            ammo += type.amount();
            notifyObservers(PlayerEvent.Type.LOOT_COLLECTED, type.amount());
        } else {
            int restored = Math.min(type.amount(), maxHealth - health);
            health += restored;
            notifyObservers(PlayerEvent.Type.HEALTH_COLLECTED, restored);
        }
        return true;
    }

    public void damage(int amount) {
        if (amount <= 0 || health <= 0) return;
        int previous = health;
        health = Math.max(0, health - amount);
        notifyObservers(PlayerEvent.Type.DAMAGED, previous - health);
        if (health == 0) notifyObservers(PlayerEvent.Type.DIED, 0);
    }
}
