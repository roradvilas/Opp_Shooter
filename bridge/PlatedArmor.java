package bridge;

/** Lets 50% of the damage through, rounding up so a hit never becomes zero. */
public final class PlatedArmor implements Armor {
    public String name() { return "Plated"; }
    public int absorb(int damage) { return (damage * 50 + 99) / 100; }
}
