public class Player {
    final int id;
    double x, y;
    int radius = 16;
    final PlayerType type;
    final int maxHealth;
    final double speed;
    int health, ammo = 60, loot = 0;
    long lastShot = 0;
    int shootDelay = 130;

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
        return ammo > 0 && now - lastShot >= shootDelay && health > 0;
    }

    public Bullet shoot(long bulletId, double targetX, double targetY, long now) {
        double dx = targetX - x, dy = targetY - y;
        double len = Math.hypot(dx, dy);
        if (len == 0) return null;
        lastShot = now;
        ammo--;
        dx /= len; dy /= len;
        return new Bullet(bulletId, id, x + dx * 20, y + dy * 20, dx * 9, dy * 9);
    }

    public void damage(int amount) { health = Math.max(0, health - amount); }
}
