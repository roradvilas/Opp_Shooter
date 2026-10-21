package facade;

import strategy.MovementType;
import strategy.WeaponType;

import java.util.Objects;

/**
 * One client's latest input snapshot as the server sees it. {@code weapon} is null when the
 * snapshot does not ask for a weapon change.
 */
public record PlayerInput(boolean w, boolean s, boolean a, boolean d, boolean shoot,
                          double mouseX, double mouseY, MovementType movement, WeaponType weapon) {
    public PlayerInput { Objects.requireNonNull(movement); }

    /** Standing still, aiming at the given point, walking. */
    public static PlayerInput idle(double mouseX, double mouseY) {
        return new PlayerInput(false, false, false, false, false, mouseX, mouseY, MovementType.WALK, null);
    }
}
