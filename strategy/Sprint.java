package strategy;

import bridge.Player;

public final class Sprint implements MovementStrategy {
    public double speedMultiplier(Player player, long now, boolean moving) { return 1.5; }
}
