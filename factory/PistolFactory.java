package factory;

import strategy.Pistol;
import strategy.WeaponStrategy;

public final class PistolFactory extends WeaponFactory {
    @Override public WeaponStrategy createWeapon() { return new Pistol(); }
}
