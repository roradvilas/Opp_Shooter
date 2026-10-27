package strategy;

import bridge.Player;
import prototype.Bullet;

import java.util.List;
import java.util.function.LongSupplier;

/** Strategy: how one weapon fires (delay between shots, bullets per shot, bullet prototype). */
public interface WeaponStrategy {
    int ASSAULT_RIFLE_DELAY = 130;
    int shootingDelay();
    int bulletCount();
    Bullet bulletPrototype();
    List<Bullet> fire(Player player, double angle, LongSupplier bulletIds);

    default Bullet bullet(Player player, double angle, LongSupplier bulletIds) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        return bulletPrototype().copy(bulletIds.getAsLong(), player.id,
                player.x + dx * 20, player.y + dy * 20, angle);
    }
}
