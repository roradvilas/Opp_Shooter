package bridge;

/** Lets 50% of the damage through, rounding up so a hit never becomes zero; costs 25% speed. */
public final class PlatedArmor implements Armor {
    private static final int MAX_DURABILITY = 120;
    private int durability = MAX_DURABILITY;

    public ArmorType type() { return ArmorType.PLATED; }
    /** A broken armor lets all of the damage through. */
    public int absorb(int damage) { return isBroken() ? damage : (damage * 50 + 99) / 100; }
    public double speedPenalty() { return isBroken() ? 0.0 : 0.25; }
    public void onHit(int damage) { durability = Math.max(0, durability - Math.max(0, damage)); }
    public int durability() { return durability; }
    public int maxDurability() { return MAX_DURABILITY; }
}
