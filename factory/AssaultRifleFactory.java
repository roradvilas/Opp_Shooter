package factory;

import strategy.AssaultRifle;
import strategy.WeaponStrategy;

public final class AssaultRifleFactory extends WeaponFactory {
    @Override public WeaponStrategy createWeapon() { return new AssaultRifle(); }
}
