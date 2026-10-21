package bridge;

/** Lets 75% of the damage through, rounding up so a hit never becomes zero. */
public final class KevlarArmor implements Armor {
    public String name() { return "Kevlar"; }
    public int absorb(int damage) { return (damage * 75 + 99) / 100; }
}
