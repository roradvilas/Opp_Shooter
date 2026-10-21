package facade;

import bridge.PlayerType;
import strategy.WeaponType;

/** A player as the client sees it in a STATE message. */
public record PlayerView(int id, double x, double y, int hp, int ammo, int loot,
                         PlayerType type, int maxHealth, double angle, WeaponType weapon) {
}
