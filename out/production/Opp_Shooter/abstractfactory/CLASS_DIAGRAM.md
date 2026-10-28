# Abstract factory UML class diagram

`CombatFactory` and `TrainingFactory` create families of player classes. Each factory builds one
heavy, normal, or light product and then uses its matching builder to equip it.

```mermaid
classDiagram
    direction TB

    class AbstractFactory {
        <<abstract>>
        +createHeavy(id, x, y, weapon, armor) Player
        +createNormal(id, x, y, weapon, armor) Player
        +createLight(id, x, y, weapon, armor) Player
        +create(type, id, x, y, weapon, armor) Player
    }
    class CombatFactory {
        +createHeavy(id, x, y, weapon, armor) Player
        +createNormal(id, x, y, weapon, armor) Player
        +createLight(id, x, y, weapon, armor) Player
    }
    class TrainingFactory {
        +createHeavy(id, x, y, weapon, armor) Player
        +createNormal(id, x, y, weapon, armor) Player
        +createLight(id, x, y, weapon, armor) Player
    }

    class Player
    class HeavyPlayer
    class NormalPlayer
    class LightPlayer
    class CombatHeavyPlayer
    class CombatNormalPlayer
    class CombatLightPlayer
    class TrainingHeavyPlayer
    class TrainingNormalPlayer
    class TrainingLightPlayer

    class PlayerBuilder {
        <<interface>>
        +start(player) PlayerBuilder
        +addWeapon(type) PlayerBuilder
        +addArmor(type) PlayerBuilder
        +addMovement() PlayerBuilder
        +addAmmo() PlayerBuilder
        +build() Player
    }
    class CombatPlayerBuilder
    class TrainingPlayerBuilder
    class WeaponType
    class ArmorType

    AbstractFactory <|-- CombatFactory
    AbstractFactory <|-- TrainingFactory

    Player <|-- HeavyPlayer
    Player <|-- NormalPlayer
    Player <|-- LightPlayer
    HeavyPlayer <|-- CombatHeavyPlayer
    NormalPlayer <|-- CombatNormalPlayer
    LightPlayer <|-- CombatLightPlayer
    HeavyPlayer <|-- TrainingHeavyPlayer
    NormalPlayer <|-- TrainingNormalPlayer
    LightPlayer <|-- TrainingLightPlayer

    CombatFactory ..> CombatHeavyPlayer : creates
    CombatFactory ..> CombatNormalPlayer : creates
    CombatFactory ..> CombatLightPlayer : creates
    TrainingFactory ..> TrainingHeavyPlayer : creates
    TrainingFactory ..> TrainingNormalPlayer : creates
    TrainingFactory ..> TrainingLightPlayer : creates

    PlayerBuilder <|.. CombatPlayerBuilder
    PlayerBuilder <|.. TrainingPlayerBuilder
    CombatFactory ..> CombatPlayerBuilder : equips products with
    TrainingFactory ..> TrainingPlayerBuilder : equips products with
    PlayerBuilder ..> Player : builds
    PlayerBuilder ..> WeaponType : selects weapon
    PlayerBuilder ..> ArmorType : selects armor
```

Both factories expose the same creation methods, but each returns products from its own family.
The combat builder equips a standard weapon, armor, walking movement, and 60 ammo. The training
builder equips a logging weapon, armor, sneaking movement, and 999 ammo; its player products also
cannot be reduced below 1 health.
