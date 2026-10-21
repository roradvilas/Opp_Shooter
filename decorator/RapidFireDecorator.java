package decorator;

import strategy.WeaponStrategy;

public final class RapidFireDecorator extends WeaponDecorator {
    public RapidFireDecorator(WeaponStrategy wrappee) { super(wrappee); }

    @Override public int shootingDelay() { return wrappee.shootingDelay() / 2; }
}
