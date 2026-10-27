# Factory method UML class diagram

`WeaponFactory` defines the factory method. Each concrete factory chooses a weapon implementation,
and `WeaponType` selects which factory to use.

```mermaid
classDiagram
    direction LR

    class WeaponFactory {
        <<abstract>>
        +createWeapon() WeaponStrategy
        +forType(type)$ WeaponFactory
    }
    class PistolFactory {
        +createWeapon() WeaponStrategy
    }
    class AssaultRifleFactory {
        +createWeapon() WeaponStrategy
    }
    class ShotgunFactory {
        +createWeapon() WeaponStrategy
    }
    class LaserFactory {
        +createWeapon() WeaponStrategy
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
    class LegacyLaserGun
    class WeaponType {
        <<enum>>
        +createStrategy() WeaponStrategy
    }
    class Bullet

    WeaponFactory <|-- PistolFactory
    WeaponFactory <|-- AssaultRifleFactory
    WeaponFactory <|-- ShotgunFactory
    WeaponFactory <|-- LaserFactory

    WeaponStrategy <|.. Pistol
    WeaponStrategy <|.. AssaultRifle
    WeaponStrategy <|.. Shotgun
    WeaponStrategy <|.. LaserGunAdapter
    WeaponFactory ..> WeaponStrategy : returns
    PistolFactory ..> Pistol : creates
    AssaultRifleFactory ..> AssaultRifle : creates
    ShotgunFactory ..> Shotgun : creates
    LaserFactory ..> LaserGunAdapter : creates
    LaserGunAdapter --> LegacyLaserGun : wraps
    WeaponType ..> WeaponFactory : selects by type
    WeaponType ..> WeaponStrategy : obtains
    WeaponStrategy ..> Bullet : fires
```

`CombatPlayerBuilder` and `TrainingPlayerBuilder` use `WeaponFactory.forType(type).createWeapon()`
when equipping a player. The laser product is a `LaserGunAdapter`, which presents the legacy laser
as a `WeaponStrategy`.
