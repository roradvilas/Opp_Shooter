package factory;

import bridge.NormalPlayer;
import bridge.Player;

public final class NormalPlayerFactory extends PlayerFactory {
    @Override public Player createPlayer(int id, double x, double y) {
        return new NormalPlayer(id, x, y);
    }
}
