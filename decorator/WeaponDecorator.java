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

    public int shootingDelay() { return wrappee.shootingDelay(); }
    public int bulletCount() { return wrappee.bulletCount(); }
    public Bullet bulletPrototype() { return wrappee.bulletPrototype(); }
    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        return wrappee.fire(player, angle, bulletIds);
    }
}
