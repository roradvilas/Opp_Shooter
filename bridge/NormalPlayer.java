package bridge;

public class NormalPlayer extends Player {
    public NormalPlayer(int id, double x, double y) {
        super(id, x, y, PlayerType.NORMAL, 100, 3.2);
    }

    /** Baseline: feels the armor's full speed penalty. */
    @Override protected double armorEncumbrance() { return 1.0; }
}
