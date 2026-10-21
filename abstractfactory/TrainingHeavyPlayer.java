package abstractfactory;

import bridge.HeavyPlayer;

/** Training dummy-proof heavy: damage can never take health below 1. */
public class TrainingHeavyPlayer extends HeavyPlayer {
    public TrainingHeavyPlayer(int id, double x, double y) { super(id, x, y); }

    @Override public void damage(int amount) { super.damage(Math.min(amount, health - 1)); }
}
