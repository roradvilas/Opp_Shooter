package bridge;

public enum PlayerType {
    HEAVY("Heavy (120 HP, 80% speed)"),
    NORMAL("Normal (100 HP, 100% speed)"),
    LIGHT("Light (80 HP, 20% faster)");

    private final String label;

    PlayerType(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
