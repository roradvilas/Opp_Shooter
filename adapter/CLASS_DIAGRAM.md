# Adapter UML class diagram

`LaserGunAdapter` implements the weapon interface expected by the game and delegates laser-specific
operations to the existing `LegacyLaserGun` API.

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
    class LaserGunAdapter {
        -laser LegacyLaserGun
        -prototype Bullet
        +LaserGunAdapter(laser)
        +shootingDelay() int
        +bulletCount() int
        +bulletPrototype() Bullet
        +fire(player, angle, bulletIds) List~Bullet~
    }
    class LegacyLaserGun {
        -charged boolean
        +chargeCapacitor() void
        +isCharged() boolean
        +rechargeTimeMs() double
        +beamSpeed() double
        +beamDamage() int
        +dischargeCapacitor() void
    }
    class Player
    class Bullet
    class WeaponFactory {
        <<abstract>>
        +createWeapon() WeaponStrategy
    }
    class LaserFactory {
        +createWeapon() WeaponStrategy
    }

    WeaponStrategy <|.. LaserGunAdapter : target interface
    LaserGunAdapter *--> LegacyLaserGun : adaptee
    LaserGunAdapter ..> Bullet : configures and fires
    WeaponStrategy ..> Player : fires for
    WeaponStrategy ..> Bullet : returns
    WeaponFactory <|-- LaserFactory
    LaserFactory ..> LaserGunAdapter : creates
    Player --> WeaponStrategy : equipped weapon
```

The adapter maps `rechargeTimeMs()` to `shootingDelay()` and uses `beamSpeed()` and `beamDamage()`
to initialize its bullet prototype. On `fire`, it charges and discharges the legacy capacitor,
then returns the projectile through the standard `WeaponStrategy` API.
