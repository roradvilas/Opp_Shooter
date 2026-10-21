package builder;

import bridge.Player;
import strategy.WeaponType;

/**
 * Builder: assembles a Player step by step. Concrete builders decide what each step means,
 * so the same call chain produces a combat or a training loadout.
 */
public interface PlayerBuilder {
    PlayerBuilder start(Player player);
    PlayerBuilder addWeapon(WeaponType type);
    PlayerBuilder addArmor();
    PlayerBuilder addMovement();
    PlayerBuilder addAmmo();
    /** Returns the finished player. */
    Player build();
}
