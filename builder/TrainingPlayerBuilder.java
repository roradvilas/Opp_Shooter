package builder;

import bridge.PlatedArmor;
import bridge.Player;
import decorator.LoggingWeaponDecorator;
import factory.WeaponFactory;
import strategy.MovementType;
import strategy.Sneak;
import strategy.WeaponType;

import java.util.Objects;

public final class TrainingPlayerBuilder implements PlayerBuilder {
    private final WeaponFactory weapons = new WeaponFactory();
    private Player player;

    @Override public PlayerBuilder start(Player player) { this.player = Objects.requireNonNull(player); return this; }

    @Override public PlayerBuilder addWeapon(WeaponType type) {
        player.equipWeapon(type, new LoggingWeaponDecorator(weapons.createWeapon(type)));
        return this;
    }

    @Override public PlayerBuilder addArmor() { player.setArmor(new PlatedArmor()); return this; }

    @Override public PlayerBuilder addMovement() { player.equipMovement(MovementType.SNEAK, new Sneak()); return this; }

    @Override public PlayerBuilder addAmmo() { player.ammo = 999; return this; }

    @Override public Player build() { return player; }
}
