public class Bullet {
    final long id;
    final int ownerId;
    double x, y, vx, vy;
    boolean alive = true;
    int damage = 25;

    public Bullet(long id, int ownerId, double x, double y, double vx, double vy) {
        this.id = id; this.ownerId = ownerId;
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
    }

    public void update(World world) {
        x += vx; y += vy;
        if (x < 0 || y < 0 || x > world.getWidth() || y > world.getHeight()
                || world.isWall((int)x, (int)y)) alive = false;
    }
}
