package factory;

import bridge.LightPlayer;
import bridge.Player;

public final class LightPlayerFactory extends PlayerFactory {
    @Override public Player createPlayer(int id, double x, double y) {
        return new LightPlayer(id, x, y);
    }
}
