public final class CombatFactory implements AbstractFactory {
    public Player createPlayer(PlayerType type, int id, double x, double y) {
        return PlayerFactory.forType(type).createPlayer(id, x, y);
    }
    public WeaponStrategy createWeapon(WeaponType type) { return new WeaponFactory().createWeapon(type); }
    public MovementStrategy createMovement() { return new Walk(); }
    public PlayerType defaultPlayerType() { return PlayerType.NORMAL; }
    public WeaponType defaultWeaponType() { return WeaponType.ASSAULT_RIFLE; }
    public MovementType defaultMovementType() { return MovementType.WALK; }
}
