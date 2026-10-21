package decorator;

import bridge.Player;
import prototype.Bullet;
import strategy.WeaponStrategy;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Decorator base: forwards every WeaponStrategy call to the wrapped weapon. */
public abstract class WeaponDecorator implements WeaponStrategy {
    protected final WeaponStrategy wrappee;

    protected WeaponDecorator(WeaponStrategy wrappee) { this.wrappee = Objects.requireNonNull(wrappee); }

    /** True if {@code weapon} is, or wraps at any depth, a decorator of the given class. */
    public static boolean isApplied(WeaponStrategy weapon, Class<? extends WeaponDecorator> decorator) {
        for (WeaponStrategy w = weapon; w instanceof WeaponDecorator d; w = d.wrappee)
            if (decorator.isInstance(d)) return true;
        return false;
    }

    public int shootingDelay() { return wrappee.shootingDelay(); }
    public int bulletCount() { return wrappee.bulletCount(); }
    public Bullet bulletPrototype() { return wrappee.bulletPrototype(); }
    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        return wrappee.fire(player, angle, bulletIds);
    }
}
