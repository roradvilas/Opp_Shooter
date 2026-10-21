package game;

public enum LootType {
    AMMO(5), HEALTH(25), RAPID_FIRE(0), DAMAGE_BOOST(0);

    private final int amount;

    LootType(int amount) { this.amount = amount; }

    public int amount() { return amount; }
}
