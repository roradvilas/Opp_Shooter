import java.util.List;
import java.util.function.LongSupplier;

public interface WeaponStrategy {
    int ASSAULT_RIFLE_DELAY = 130;
    int shootingDelay();
    int bulletCount();
    List<Bullet> fire(Player player, double angle, LongSupplier bulletIds);

    default Bullet bullet(Player player, double angle, LongSupplier bulletIds) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        return new Bullet(bulletIds.getAsLong(), player.id,
                player.x + dx * 20, player.y + dy * 20, dx * 9, dy * 9);
    }
}
