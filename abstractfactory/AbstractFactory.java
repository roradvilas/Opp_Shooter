package abstractfactory;

import bridge.Player;
import bridge.PlayerType;

/** Creates one family of related players; each concrete factory yields only its own family's classes. */
public abstract class AbstractFactory {
    public abstract Player createHeavy(int id, double x, double y);
    public abstract Player createNormal(int id, double x, double y);
    public abstract Player createLight(int id, double x, double y);

    public Player create(PlayerType type, int id, double x, double y) {
        return switch (type) {
            case HEAVY -> createHeavy(id, x, y);
            case NORMAL -> createNormal(id, x, y);
            case LIGHT -> createLight(id, x, y);
        };
    }
}
