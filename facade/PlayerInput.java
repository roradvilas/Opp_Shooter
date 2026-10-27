package facade;

import strategy.MovementType;

import java.util.Objects;

/**
 * One client's latest input snapshot as the server sees it.
 */
public record PlayerInput(boolean w, boolean s, boolean a, boolean d, boolean shoot,
                          double mouseX, double mouseY, MovementType movement) {
    public PlayerInput { Objects.requireNonNull(movement); }

    /** Standing still, aiming at the given point, walking. */
    public static PlayerInput idle(double mouseX, double mouseY) {
        return new PlayerInput(false, false, false, false, false, mouseX, mouseY, MovementType.WALK);
    }
}
