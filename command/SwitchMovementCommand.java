package command;

import bridge.Player;
import strategy.MovementStrategy;
import strategy.MovementType;

import java.util.Objects;

public final class SwitchMovementCommand implements Command {
    private final Player player;
    private final MovementType type;
    private MovementType oldType;
    private MovementStrategy oldStrategy;

    public SwitchMovementCommand(Player player, MovementType type) {
        this.player = Objects.requireNonNull(player);
        this.type = Objects.requireNonNull(type);
    }

    @Override public String execute() {
        oldType = player.getMovementType();
        oldStrategy = player.getMovement();
        player.setMovement(type);
        return "Player " + player.id + " movement " + oldType + " -> " + type;
    }

    @Override public String undo() {
        player.equipMovement(oldType, oldStrategy);
        return "Player " + player.id + " movement restored to " + oldType;
    }
}
