package abstractfactory;

import bridge.LightPlayer;

/** Training light: damage can never take health below 1. */
public class TrainingLightPlayer extends LightPlayer {
    public TrainingLightPlayer(int id, double x, double y) { super(id, x, y); }

    @Override public void damage(int amount) { super.damage(Math.min(amount, health - 1)); }
}
