package factory;

import adapter.LaserGunAdapter;
import adapter.LegacyLaserGun;
import strategy.AssaultRifle;
import strategy.Pistol;
import strategy.Shotgun;
import strategy.WeaponStrategy;
import strategy.WeaponType;

import java.util.Objects;

/** Factory for the concrete weapon products. */
public final class WeaponFactory {
    public WeaponStrategy createWeapon(WeaponType type) {
        return switch (Objects.requireNonNull(type)) {
            case PISTOL -> new Pistol();
            case ASSAULT_RIFLE -> new AssaultRifle();
            case SHOTGUN -> new Shotgun();
            case LASER -> new LaserGunAdapter(new LegacyLaserGun());
        };
    }
}
