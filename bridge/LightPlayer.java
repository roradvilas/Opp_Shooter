package bridge;

public class LightPlayer extends Player {
    public LightPlayer(int id, double x, double y) {
        super(id, x, y, PlayerType.LIGHT, 80, 3.2 * 1.2);
    }
}
