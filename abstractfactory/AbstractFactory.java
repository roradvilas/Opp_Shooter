package abstractfactory;

import bridge.ArmorType;
import bridge.Player;
import bridge.PlayerType;
import strategy.WeaponType;

/**
 * Creates one family of related, fully equipped players; each concrete factory yields only its own
 * family's classes and drives its own {@code PlayerBuilder} to equip them.
 */
public abstract class AbstractFactory {
    public abstract Player createHeavy(int id, double x, double y, WeaponType weapon, ArmorType armor);
    public abstract Player createNormal(int id, double x, double y, WeaponType weapon, ArmorType armor);
    public abstract Player createLight(int id, double x, double y, WeaponType weapon, ArmorType armor);

    public Player create(PlayerType type, int id, double x, double y, WeaponType weapon, ArmorType armor) {
        return switch (type) {
            case HEAVY -> createHeavy(id, x, y, weapon, armor);
            case NORMAL -> createNormal(id, x, y, weapon, armor);
            case LIGHT -> createLight(id, x, y, weapon, armor);
        };
    }
}
