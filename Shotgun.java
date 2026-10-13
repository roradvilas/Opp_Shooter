import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

public final class Shotgun implements WeaponStrategy {
    public int shootingDelay() { return (int)(ASSAULT_RIFLE_DELAY / 0.2); }
    public int bulletCount() { return 3; }
    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        double left = Math.toRadians(ThreadLocalRandom.current().nextDouble(3, 15));
        double right = Math.toRadians(ThreadLocalRandom.current().nextDouble(3, 15));
        return List.of(bullet(player, angle - left, bulletIds),
                bullet(player, angle, bulletIds), bullet(player, angle + right, bulletIds));
    }
}
