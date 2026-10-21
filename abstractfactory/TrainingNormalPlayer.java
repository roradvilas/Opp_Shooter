package abstractfactory;

import bridge.NormalPlayer;

/** Training normal: damage can never take health below 1. */
public class TrainingNormalPlayer extends NormalPlayer {
    public TrainingNormalPlayer(int id, double x, double y) { super(id, x, y); }

    @Override public void damage(int amount) { super.damage(Math.min(amount, health - 1)); }
}
