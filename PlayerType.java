public enum PlayerType {
    HEAVY("Heavy (100 HP, normal speed)"),
    LIGHT("Light (80 HP, 20% faster)");

    private final String label;

    PlayerType(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
