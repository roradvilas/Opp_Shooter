package decorator;

import bridge.Player;
import prototype.Bullet;
import strategy.WeaponStrategy;

import java.util.List;
import java.util.function.LongSupplier;

public final class DamageBoostDecorator extends WeaponDecorator {
    public static final int BONUS = 10;

    public DamageBoostDecorator(WeaponStrategy wrappee) { super(wrappee); }

    @Override public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        List<Bullet> bullets = super.fire(player, angle, bulletIds);
        for (Bullet bullet : bullets) bullet.damage += BONUS;
        return bullets;
    }
}
