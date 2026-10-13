import java.util.List;
import java.util.function.LongSupplier;

public final class AssaultRifle implements WeaponStrategy {
    public int shootingDelay() { return ASSAULT_RIFLE_DELAY; }
    public int bulletCount() { return 1; }
    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        return List.of(bullet(player, angle, bulletIds));
    }
}
