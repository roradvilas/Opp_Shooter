package observer;

public record PlayerEvent(int playerId, Type type, int amount) {
    public enum Type { DAMAGED, DIED, LOOT_COLLECTED, HEALTH_COLLECTED, AMMO_EMPTY, POWERUP, ARMOR_BROKEN, UNDO_COMPLETED }

    public String message() {
        return switch (type) {
            case DAMAGED -> "Hit! -" + amount + " HP";
            case DIED -> "You died";
            case LOOT_COLLECTED -> "Loot collected! +" + amount + " ammo";
            case HEALTH_COLLECTED -> "Health collected! +" + amount + " HP";
            case AMMO_EMPTY -> "Out of ammo";
            case POWERUP -> "Power-up!";
            case ARMOR_BROKEN -> "Armor broken!";
            case UNDO_COMPLETED -> "Invalid packet: undid last " + amount + (amount == 1 ? " command" : " commands");
        };
    }
}
