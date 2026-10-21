package abstractfactory;

import bridge.Player;

public final class TrainingFactory extends AbstractFactory {
    @Override public Player createHeavy(int id, double x, double y) { return new TrainingHeavyPlayer(id, x, y); }
    @Override public Player createNormal(int id, double x, double y) { return new TrainingNormalPlayer(id, x, y); }
    @Override public Player createLight(int id, double x, double y) { return new TrainingLightPlayer(id, x, y); }
}
