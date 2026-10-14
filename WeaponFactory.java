import java.util.Objects;

/** Factory for the three concrete weapon products. */
public final class WeaponFactory {
    public WeaponStrategy createWeapon(WeaponType type) {
        return switch (Objects.requireNonNull(type)) {
            case PISTOL -> new Pistol();
            case ASSAULT_RIFLE -> new AssaultRifle();
            case SHOTGUN -> new Shotgun();
        };
    }
}
