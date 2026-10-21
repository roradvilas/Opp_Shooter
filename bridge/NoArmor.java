package bridge;

public final class NoArmor implements Armor {
    public String name() { return "None"; }
    public int absorb(int damage) { return damage; }
}
