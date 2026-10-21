package command;

import bridge.Player;
import strategy.WeaponStrategy;
import strategy.WeaponType;

import java.util.Objects;

public final class SwitchWeaponCommand implements Command {
    private final Player player;
    private final WeaponType type;
    private WeaponType oldType;
    private WeaponStrategy oldStrategy;

    public SwitchWeaponCommand(Player player, WeaponType type) {
        this.player = Objects.requireNonNull(player);
        this.type = Objects.requireNonNull(type);
    }

    @Override public String execute() {
        oldType = player.weaponType;
        oldStrategy = player.getWeapon();
        player.setWeapon(type);
        return "Player " + player.id + " weapon " + oldType + " -> " + type;
    }

    @Override public String undo() {
        player.equipWeapon(oldType, oldStrategy);
        return "Player " + player.id + " weapon restored to " + oldType;
    }
}
