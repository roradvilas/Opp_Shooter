package factory;

import adapter.LaserGunAdapter;
import adapter.LegacyLaserGun;
import strategy.WeaponStrategy;

public final class LaserFactory extends WeaponFactory {
    @Override public WeaponStrategy createWeapon() { return new LaserGunAdapter(new LegacyLaserGun()); }
}
