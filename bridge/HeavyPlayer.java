package bridge;

public class HeavyPlayer extends Player {
    public HeavyPlayer(int id, double x, double y) {
        super(id, x, y, PlayerType.HEAVY, 120, 3.2 * 0.8);
    }
}
