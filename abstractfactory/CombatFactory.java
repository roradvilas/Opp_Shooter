package abstractfactory;

import bridge.Player;

public final class CombatFactory extends AbstractFactory {
    @Override public Player createHeavy(int id, double x, double y) { return new CombatHeavyPlayer(id, x, y); }
    @Override public Player createNormal(int id, double x, double y) { return new CombatNormalPlayer(id, x, y); }
    @Override public Player createLight(int id, double x, double y) { return new CombatLightPlayer(id, x, y); }
}
