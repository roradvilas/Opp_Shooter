package prototype;

import game.World;

import java.util.Objects;

public class Bullet implements Cloneable {
    public long id;
    public int ownerId;
    public double x, y, vx, vy;
    public boolean alive = true;
    public int damage = 25;
    private BulletModifiers modifiers = new BulletModifiers();

    private static volatile CopyMode copyMode = CopyMode.DEEP;

    public Bullet(long id, int ownerId, double x, double y, double vx, double vy) {
        this.id = id; this.ownerId = ownerId;
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
    }

    public static CopyMode getCopyMode() { return copyMode; }
    public static void setCopyMode(CopyMode mode) { copyMode = Objects.requireNonNull(mode); }

    public BulletModifiers modifiers() { return modifiers; }
    public int effectiveDamage() { return damage + modifiers.getDamageBonus(); }

    /** Field-for-field copy; the clone and this bullet share one {@link BulletModifiers}. */
    public Bullet shallowCopy() {
        try {
            return (Bullet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /** Like {@link #shallowCopy()}, but the clone owns its own {@link BulletModifiers}. */
    public Bullet deepCopy() {
        Bullet copy = shallowCopy();
        copy.modifiers = modifiers.copy();
        return copy;
    }

    /** Copies this prototype into a fresh projectile with its own identity, using the current {@link CopyMode}. */
    public Bullet copy(long id, int ownerId, double x, double y, double angle) {
        Bullet copy = copyMode == CopyMode.SHALLOW ? shallowCopy() : deepCopy();
        double speed = Math.hypot(vx, vy);
        copy.id = id;
        copy.ownerId = ownerId;
        copy.x = x;
        copy.y = y;
        copy.alive = true;
        copy.vx = Math.cos(angle) * speed;
        copy.vy = Math.sin(angle) * speed;
        return copy;
    }

    public void update(World world) {
        x += vx; y += vy;
        if (x < 0 || y < 0 || x > world.getWidth() || y > world.getHeight()
                || world.isWall((int)x, (int)y)) alive = false;
    }
}
