package strategy;

import bridge.Player;

public final class Sneak implements MovementStrategy {
    public double speedMultiplier(Player player, long now, boolean moving) { return 0.5; }
}
