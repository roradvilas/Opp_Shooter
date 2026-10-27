package abstractfactory;

import bridge.ArmorType;
import bridge.Player;
import builder.CombatPlayerBuilder;
import strategy.WeaponType;

public final class CombatFactory extends AbstractFactory {
    @Override public Player createHeavy(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new CombatHeavyPlayer(id, x, y), weapon, armor); }
    @Override public Player createNormal(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new CombatNormalPlayer(id, x, y), weapon, armor); }
    @Override public Player createLight(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new CombatLightPlayer(id, x, y), weapon, armor); }

    /** Builder: a fresh {@link CombatPlayerBuilder} per player, since builders hold the player under construction. */
    private static Player equip(Player player, WeaponType weapon, ArmorType armor) {
        return new CombatPlayerBuilder().start(player).addWeapon(weapon).addArmor(armor).addMovement().addAmmo().build();
    }
}
