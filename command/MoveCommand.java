package command;

import bridge.Player;
import game.World;

import java.util.Objects;

public final class MoveCommand implements Command {
    private final Player player;
    private final boolean w, s, a, d;
    private final World world;
    private final long now;
    private double oldX, oldY;

    public MoveCommand(Player player, boolean w, boolean s, boolean a, boolean d, World world, long now) {
        this.player = Objects.requireNonNull(player);
        this.w = w; this.s = s; this.a = a; this.d = d;
        this.world = Objects.requireNonNull(world);
        this.now = now;
    }

    @Override public String execute() {
        oldX = player.x;
        oldY = player.y;
        player.move(w, s, a, d, world, now);
        return String.format("Player %d moved (%.1f, %.1f) -> (%.1f, %.1f)", player.id, oldX, oldY, player.x, player.y);
    }

    @Override public String undo() {
        double fromX = player.x, fromY = player.y;
        player.x = oldX;
        player.y = oldY;
        return String.format("Player %d move undone (%.1f, %.1f) -> (%.1f, %.1f)", player.id, fromX, fromY, oldX, oldY);
    }
}
