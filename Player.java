import java.awt.*;

public class Player {
    final int id;
    double x, y;
    int radius = 16;
    int health = 100, ammo = 60, loot = 0;
    long lastShot = 0;
    int shootDelay = 130;

    public Player(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void move(boolean w, boolean s, boolean a, boolean d, World world) {
        double dx = (d ? 1 : 0) - (a ? 1 : 0);
        double dy = (s ? 1 : 0) - (w ? 1 : 0);
        if (dx == 0 && dy == 0) return;

        double len = Math.sqrt(dx * dx + dy * dy);
        dx = dx / len * 3.2;
        dy = dy / len * 3.2;

        if (!world.collidesWithWall(x + dx, y, radius)) x += dx;
        if (!world.collidesWithWall(x, y + dy, radius)) y += dy;
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
