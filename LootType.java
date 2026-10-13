public enum LootType {
    AMMO(5), HEALTH(25);

    private final int amount;

    LootType(int amount) { this.amount = amount; }

    public int amount() { return amount; }
}
