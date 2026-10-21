package factory;

import bridge.HeavyPlayer;
import bridge.Player;

public final class HeavyPlayerFactory extends PlayerFactory {
    @Override public Player createPlayer(int id, double x, double y) {
        return new HeavyPlayer(id, x, y);
    }
}
