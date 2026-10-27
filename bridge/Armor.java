package bridge;

/**
 * Bridge implementor: the primitive operations a player's armor provides, independent of the player
 * class. A player class combines these primitives in its own way (see {@link Player#armorEncumbrance()}).
 */
public interface Armor {
    ArmorType type();
    /** Returns the damage that gets through the armor; a broken armor lets all of it through. */
    int absorb(int damage);
    /** Fraction of movement speed the armor costs a fully encumbered wearer, from 0 (none) to 1. */
    double speedPenalty();
    /** Wears the armor down after the wearer took a hit of {@code damage} (before absorption). */
    void onHit(int damage);
    int durability();
    int maxDurability();

    /** True once armor that has durability to lose is used up; having no armor at all is not "broken". */
    default boolean isBroken() { return maxDurability() > 0 && durability() == 0; }
}
