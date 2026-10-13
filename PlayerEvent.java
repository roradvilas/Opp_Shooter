public record PlayerEvent(int playerId, Type type, int amount) {
    public enum Type { DAMAGED, DIED, LOOT_COLLECTED, HEALTH_COLLECTED, AMMO_EMPTY }

    public String message() {
        return switch (type) {
            case DAMAGED -> "Hit! -" + amount + " HP";
            case DIED -> "You died";
            case LOOT_COLLECTED -> "Loot collected! +" + amount + " ammo";
            case HEALTH_COLLECTED -> "Health collected! +" + amount + " HP";
            case AMMO_EMPTY -> "Out of ammo";
        };
    }
}
