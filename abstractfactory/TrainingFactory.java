package abstractfactory;

import bridge.ArmorType;
import bridge.Player;
import builder.TrainingPlayerBuilder;
import strategy.WeaponType;

public final class TrainingFactory extends AbstractFactory {
    @Override public Player createHeavy(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new TrainingHeavyPlayer(id, x, y), weapon, armor); }
    @Override public Player createNormal(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new TrainingNormalPlayer(id, x, y), weapon, armor); }
    @Override public Player createLight(int id, double x, double y, WeaponType weapon, ArmorType armor) { return equip(new TrainingLightPlayer(id, x, y), weapon, armor); }

    /** Builder: a fresh {@link TrainingPlayerBuilder} per player, since builders hold the player under construction. */
    private static Player equip(Player player, WeaponType weapon, ArmorType armor) {
        return new TrainingPlayerBuilder().start(player).addWeapon(weapon).addArmor(armor).addMovement().addAmmo().build();
    }
}
