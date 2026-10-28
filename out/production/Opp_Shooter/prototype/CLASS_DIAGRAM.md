# Prototype UML class diagram

`Bullet` acts as its own prototype. It can make a shallow clone, a deep clone, or a fresh projectile
copy using the configured `CopyMode`.

```mermaid
classDiagram
    direction LR

    class Bullet {
        <<prototype>>
        -modifiers BulletModifiers
        +shallowCopy() Bullet
        +deepCopy() Bullet
        +copy(id, ownerId, x, y, angle) Bullet
        +getCopyMode() CopyMode$
        +setCopyMode(mode) void$
        +effectiveDamage() int
    }
    class BulletModifiers {
        -damageBonus int
        +getDamageBonus() int
        +setDamageBonus(damageBonus) void
        +copy() BulletModifiers
    }
    class CopyMode {
        <<enum>>
        SHALLOW
        DEEP
    }
    class PrototypeDemo

    Bullet *--> BulletModifiers : nested mutable state
    Bullet ..> CopyMode : chooses copy behavior
    PrototypeDemo ..> Bullet : compares copies
```

`shallowCopy()` creates a new `Bullet` that shares the original's `BulletModifiers`. `deepCopy()`
also clones the modifiers, so changes to one copy do not affect the other. `copy()` selects between
those methods using `CopyMode`, then assigns the new projectile's identity, position, and velocity.
