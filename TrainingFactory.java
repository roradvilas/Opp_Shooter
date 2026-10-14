public final class TrainingFactory implements AbstractFactory {
    public Player createPlayer(PlayerType type, int id, double x, double y) {
        return PlayerFactory.forType(type).createPlayer(id, x, y);
    }
    public WeaponStrategy createWeapon(WeaponType type) { return new WeaponFactory().createWeapon(type); }
    public MovementStrategy createMovement() { return new Sneak(); }
    public PlayerType defaultPlayerType() { return PlayerType.LIGHT; }
    public WeaponType defaultWeaponType() { return WeaponType.PISTOL; }
    public MovementType defaultMovementType() { return MovementType.SNEAK; }
}
