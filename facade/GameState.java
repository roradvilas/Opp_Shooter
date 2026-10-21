package facade;

import game.Loot;

import java.util.List;
import java.util.Map;

/** One decoded STATE message: everything a client needs to draw a frame. */
public record GameState(Map<Integer, PlayerView> players, List<BulletView> bullets, List<Loot> loot) {
    public GameState {
        players = Map.copyOf(players);
        bullets = List.copyOf(bullets);
        loot = List.copyOf(loot);
    }
}
