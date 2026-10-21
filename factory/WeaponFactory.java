package factory;

import strategy.WeaponStrategy;
import strategy.WeaponType;

import java.util.Objects;

/** Factory Method creator: each subclass decides which concrete weapon {@link #createWeapon()} returns. */
public abstract class WeaponFactory {
    public abstract WeaponStrategy createWeapon();

    public static WeaponFactory forType(WeaponType type) {
        return switch (Objects.requireNonNull(type)) {
            case PISTOL -> new PistolFactory();
            case ASSAULT_RIFLE -> new AssaultRifleFactory();
            case SHOTGUN -> new ShotgunFactory();
            case LASER -> new LaserFactory();
        };
    }
}
