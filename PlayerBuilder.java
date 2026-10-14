import java.util.Objects;

/** Common construction steps. Each build creates an independent playable player. */
public abstract class PlayerBuilder {
    private final AbstractFactory factory;
    private PlayerType playerType;
    private WeaponType weaponType;
    private int id;
    private double x, y;

    protected PlayerBuilder(AbstractFactory factory) {
        this.factory = Objects.requireNonNull(factory);
        playerType = factory.defaultPlayerType();
        weaponType = factory.defaultWeaponType();
    }

    public PlayerBuilder playerType(PlayerType type) { playerType = Objects.requireNonNull(type); return this; }
    public PlayerBuilder weaponType(WeaponType type) { weaponType = Objects.requireNonNull(type); return this; }
    public PlayerBuilder identity(int id) { this.id = id; return this; }
    public PlayerBuilder position(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Position must be finite");
        this.x = x; this.y = y; return this;
    }

    public Player build() {
        Player player = factory.createPlayer(playerType, id, x, y);
        player.equipWeapon(weaponType, factory.createWeapon(weaponType));
        player.equipMovement(factory.defaultMovementType(), factory.createMovement());
        return player;
    }
}
