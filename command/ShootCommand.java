package command;

import bridge.Player;
import prototype.Bullet;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class ShootCommand implements Command {
    private final Player player;
    private final LongSupplier ids;
    private final double targetX, targetY;
    private final long now;
    private final List<Bullet> out;
    private int oldAmmo;
    private long oldLastShot;
    private List<Bullet> fired = List.of();

    public ShootCommand(Player player, LongSupplier ids, double targetX, double targetY, long now, List<Bullet> out) {
        this.player = Objects.requireNonNull(player);
        this.ids = Objects.requireNonNull(ids);
        this.targetX = targetX;
        this.targetY = targetY;
        this.now = now;
        this.out = Objects.requireNonNull(out);
    }

    @Override public String execute() {
        oldAmmo = player.ammo;
        oldLastShot = player.lastShot;
        fired = player.shoot(ids, targetX, targetY, now);
        out.addAll(fired);
        return "Player " + player.id + " fired " + fired.size() + " bullet(s), ammo " + oldAmmo + " -> " + player.ammo;
    }

    @Override public String undo() {
        for (Bullet bullet : fired) bullet.alive = false;
        int removed = fired.size();
        fired = List.of();
        player.ammo = oldAmmo;
        player.lastShot = oldLastShot;
        return "Player " + player.id + " shot undone, " + removed + " bullet(s) removed, ammo " + oldAmmo;
    }
}
