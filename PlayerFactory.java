public abstract class PlayerFactory {
    public abstract Player createPlayer(int id, double x, double y);

    public static PlayerFactory forType(PlayerType type) {
        return switch (type) {
            case HEAVY -> new HeavyPlayerFactory();
            case NORMAL -> new NormalPlayerFactory();
            case LIGHT -> new LightPlayerFactory();
        };
    }
}
