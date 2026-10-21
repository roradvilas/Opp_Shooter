package strategy;

import bridge.Player;

public final class Walk implements MovementStrategy {
    public double speedMultiplier(Player player, long now, boolean moving) { return 1.0; }
}
