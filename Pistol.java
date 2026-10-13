import java.util.List;
import java.util.function.LongSupplier;

public final class Pistol implements WeaponStrategy {
    public int shootingDelay() { return (int)(ASSAULT_RIFLE_DELAY / 0.4); }
    public int bulletCount() { return 1; }
    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        return List.of(bullet(player, angle, bulletIds));
    }
}
