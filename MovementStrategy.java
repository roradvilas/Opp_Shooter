/** Player is the context; each instance supplies the current movement speed. */
public interface MovementStrategy {
    double speedMultiplier(Player player, long now, boolean moving);
}
