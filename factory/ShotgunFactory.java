package factory;

import strategy.Shotgun;
import strategy.WeaponStrategy;

public final class ShotgunFactory extends WeaponFactory {
    @Override public WeaponStrategy createWeapon() { return new Shotgun(); }
}
