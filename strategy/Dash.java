package strategy;

import bridge.Player;

/** A short burst followed by walking until the next dash is available. */
public final class Dash implements MovementStrategy {
    public static final long DURATION_MS = 150;
    public static final long COOLDOWN_MS = 1000;

    public double speedMultiplier(Player player, long now, boolean moving) {
        if (moving && now >= player.nextDashAt) {
            player.dashUntil = now + DURATION_MS;
            player.nextDashAt = now + COOLDOWN_MS;
        }
        return moving && now < player.dashUntil ? 3.0 : 1.0;
    }
}
