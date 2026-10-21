package builder;

import bridge.KevlarArmor;
import bridge.Player;
import factory.WeaponFactory;
import strategy.MovementType;
import strategy.Walk;
import strategy.WeaponType;

import java.util.Objects;

public final class CombatPlayerBuilder implements PlayerBuilder {
    private Player player;

    @Override public PlayerBuilder start(Player player) { this.player = Objects.requireNonNull(player); return this; }

    @Override public PlayerBuilder addWeapon(WeaponType type) {
        player.equipWeapon(type, WeaponFactory.forType(type).createWeapon());
        return this;
    }

    @Override public PlayerBuilder addArmor() { player.setArmor(new KevlarArmor()); return this; }

    @Override public PlayerBuilder addMovement() { player.equipMovement(MovementType.WALK, new Walk()); return this; }

    @Override public PlayerBuilder addAmmo() { player.ammo = 60; return this; }

    @Override public Player build() { return player; }
}
