# Decorator UML class diagram

Each decorator implements the same `WeaponStrategy` interface as a weapon and wraps another
`WeaponStrategy`. This allows decorators to be stacked while a `Player` continues to use one
weapon interface.

```mermaid
classDiagram
    direction LR

    class WeaponStrategy {
        <<interface>>
        +shootingDelay() int
        +bulletCount() int
        +bulletPrototype() Bullet
        +fire(player, angle, bulletIds) List~Bullet~
    }
    class WeaponDecorator {
        <<abstract>>
        #wrappee WeaponStrategy
        #WeaponDecorator(wrappee)
        +isApplied(weapon, decorator)$ boolean
        +shootingDelay() int
        +bulletCount() int
        +bulletPrototype() Bullet
        +fire(player, angle, bulletIds) List~Bullet~
    }
    class RapidFireDecorator {
        +shootingDelay() int
    }
    class DamageBoostDecorator {
        +BONUS int
        +fire(player, angle, bulletIds) List~Bullet~
    }
    class LoggingWeaponDecorator {
        +fire(player, angle, bulletIds) List~Bullet~
    }

    class Pistol
    class AssaultRifle
    class Shotgun
    class LaserGunAdapter
    class Player {
        -weapon WeaponStrategy
        +equipWeapon(type, strategy) void
        +shoot(bulletIds, targetX, targetY, now) List~Bullet~
        +collectLoot(type) boolean
    }
    class Bullet
    class Logger
    class TrainingPlayerBuilder

    WeaponStrategy <|.. WeaponDecorator
    WeaponStrategy <|.. Pistol
    WeaponStrategy <|.. AssaultRifle
    WeaponStrategy <|.. Shotgun
    WeaponStrategy <|.. LaserGunAdapter
    WeaponDecorator <|-- RapidFireDecorator
    WeaponDecorator <|-- DamageBoostDecorator
    WeaponDecorator <|-- LoggingWeaponDecorator
    WeaponDecorator o--> WeaponStrategy : wraps

    Player --> WeaponStrategy : equipped weapon
    Player ..> RapidFireDecorator : applies power-up
    Player ..> DamageBoostDecorator : applies power-up
    TrainingPlayerBuilder ..> LoggingWeaponDecorator : wraps training weapon
    DamageBoostDecorator ..> Bullet : adds damage bonus
    LoggingWeaponDecorator ..> Logger : logs firing
```

The wrapped reference can itself be another decorator. For example,
`LoggingWeaponDecorator(DamageBoostDecorator(RapidFireDecorator(Pistol)))` remains a
`WeaponStrategy`. The decorators change firing delay, bullet damage, or logging while forwarding
the other weapon operations to the wrapped object.
