package bridge;

/** Bridge implementor: how incoming damage is reduced, independent of the player class. */
public interface Armor {
    String name();
    /** Returns the damage that gets through the armor. */
    int absorb(int damage);
}
