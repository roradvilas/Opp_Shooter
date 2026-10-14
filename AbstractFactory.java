/** Creates a related loadout family using the player and weapon factories. */
public interface AbstractFactory {
    Player createPlayer(PlayerType type, int id, double x, double y);
    WeaponStrategy createWeapon(WeaponType type);
    MovementStrategy createMovement();
    PlayerType defaultPlayerType();
    WeaponType defaultWeaponType();
    MovementType defaultMovementType();
}
