package bridge;

public class LightPlayer extends Player {
    public LightPlayer(int id, double x, double y) {
        super(id, x, y, PlayerType.LIGHT, 80, 3.2 * 1.2);
    }

    /** Not built for armor: feels one and a half times the penalty. */
    @Override protected double armorEncumbrance() { return 1.5; }
}
