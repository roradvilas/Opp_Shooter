# Strategy UML class diagram

`Player` delegates two kinds of behavior to replaceable strategies: movement and weapon firing.
Each strategy interface has its own concrete implementations.

```mermaid
classDiagram
    direction LR

    class Player {
        -movement MovementStrategy
        -movementType MovementType
        -weapon WeaponStrategy
        -weaponType WeaponType
        +equipMovement(type, strategy) void
        +equipWeapon(type, strategy) void
        +move() void
        +shoot() List~Bullet~
    }

    class MovementStrategy {
        <<interface>>
        +speedMultiplier(player, now, moving) double
    }
    class Sneak
    class Walk
    class Sprint
    class Dash
    class MovementType {
        <<enum>>
        +createStrategy() MovementStrategy
    }

    class WeaponStrategy {
        <<interface>>
        +shootingDelay() int
        +bulletCount() int
        +bulletPrototype() Bullet
        +fire(player, angle, bulletIds) List~Bullet~
    }
    class Pistol
    class AssaultRifle
    class Shotgun
    class LaserGunAdapter
    class WeaponType {
        <<enum>>
        +createStrategy() WeaponStrategy
    }
    class WeaponFactory {
        <<abstract>>
        +createWeapon() WeaponStrategy
        +forType(type)$ WeaponFactory
    }
    class Bullet

    Player --> MovementStrategy : delegates movement
    Player --> MovementType : selects movement
    MovementStrategy <|.. Sneak
    MovementStrategy <|.. Walk
    MovementStrategy <|.. Sprint
    MovementStrategy <|.. Dash
    MovementType ..> MovementStrategy : creates
    MovementStrategy ..> Player : uses movement state

    Player --> WeaponStrategy : delegates firing
    Player --> WeaponType : selects weapon
    WeaponStrategy <|.. Pistol
    WeaponStrategy <|.. AssaultRifle
    WeaponStrategy <|.. Shotgun
    WeaponStrategy <|.. LaserGunAdapter
    WeaponStrategy ..> Player : fires for
    WeaponStrategy ..> Bullet : creates
    WeaponType ..> WeaponFactory : selects factory
    WeaponFactory ..> WeaponStrategy : creates
```

`MovementType` creates the selected movement strategy. `WeaponType` selects a factory, which creates
the corresponding weapon strategy. `LaserGunAdapter` also implements `WeaponStrategy`, allowing the
legacy laser to be equipped through the same interface.
