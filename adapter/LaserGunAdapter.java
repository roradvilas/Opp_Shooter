package adapter;

import bridge.Player;
import prototype.Bullet;
import strategy.WeaponStrategy;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Adapter: presents a {@link LegacyLaserGun} as a {@link WeaponStrategy}. */
public final class LaserGunAdapter implements WeaponStrategy {
    private final LegacyLaserGun laser;
    private final Bullet prototype;

    public LaserGunAdapter(LegacyLaserGun laser) {
        this.laser = Objects.requireNonNull(laser);
        prototype = new Bullet(0, 0, 0, 0, laser.beamSpeed(), 0);
        prototype.damage = laser.beamDamage();
    }

    public int shootingDelay() { return (int) laser.rechargeTimeMs(); }
    public int bulletCount() { return 1; }
    public Bullet bulletPrototype() { return prototype; }

    public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        laser.chargeCapacitor();
        return laser.isCharged() ? List.of(bullet(player, angle, bulletIds)) : List.of();
    }
}
