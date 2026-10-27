package bridge;

public enum ArmorType {
    NONE("None (no protection, full speed)"),
    KEVLAR("Kevlar (75% damage, -10% speed, durability 80)"),
    PLATED("Plated (50% damage, -25% speed, durability 120)");

    private final String label;

    ArmorType(String label) { this.label = label; }

    /** A fresh, undamaged armor of this type. */
    public Armor create() {
        return switch (this) {
            case NONE -> new NoArmor();
            case KEVLAR -> new KevlarArmor();
            case PLATED -> new PlatedArmor();
        };
    }

    @Override public String toString() { return label; }
}
