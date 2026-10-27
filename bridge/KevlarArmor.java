package bridge;

/** Lets 75% of the damage through, rounding up so a hit never becomes zero; costs 10% speed. */
public final class KevlarArmor implements Armor {
    private static final int MAX_DURABILITY = 80;
    private int durability = MAX_DURABILITY;

    public ArmorType type() { return ArmorType.KEVLAR; }
    /** A broken armor lets all of the damage through. */
    public int absorb(int damage) { return isBroken() ? damage : (damage * 75 + 99) / 100; }
    public double speedPenalty() { return isBroken() ? 0.0 : 0.10; }
    public void onHit(int damage) { durability = Math.max(0, durability - Math.max(0, damage)); }
    public int durability() { return durability; }
    public int maxDurability() { return MAX_DURABILITY; }
}
