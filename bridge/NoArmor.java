package bridge;

public final class NoArmor implements Armor {
    public ArmorType type() { return ArmorType.NONE; }
    public int absorb(int damage) { return damage; }
    public double speedPenalty() { return 0.0; }
    public void onHit(int damage) {}
    public int durability() { return 0; }
    public int maxDurability() { return 0; }
}
