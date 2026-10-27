# Bridge UML class diagram

The `Player` abstraction delegates armor behavior to an `Armor` implementor. Player classes and
armor classes can vary independently; each player chooses an armor through `setArmor`.

```mermaid
classDiagram
    direction LR

    class Player {
        <<abstraction>>
        -armor Armor
        +getArmor() Armor
        +setArmor(armor) void
        +damage(amount) void
        +effectiveSpeed() double
        #armorEncumbrance() double
    }
    class HeavyPlayer {
        +armorEncumbrance() double
    }
    class NormalPlayer {
        +armorEncumbrance() double
    }
    class LightPlayer {
        +armorEncumbrance() double
    }
    class PlayerType {
        <<enum>>
    }

    class Armor {
        <<interface>>
        +type() ArmorType
        +absorb(damage) int
        +speedPenalty() double
        +onHit(damage) void
        +durability() int
        +maxDurability() int
        +isBroken() boolean
    }
    class NoArmor
    class KevlarArmor
    class PlatedArmor
    class ArmorType {
        <<enum>>
        +create() Armor
    }

    Player <|-- HeavyPlayer
    Player <|-- NormalPlayer
    Player <|-- LightPlayer
    Player o--> Armor : delegates armor behavior to
    Player --> PlayerType : player class

    Armor <|.. NoArmor
    Armor <|.. KevlarArmor
    Armor <|.. PlatedArmor
    ArmorType ..> Armor : creates
    Armor ..> ArmorType : reports type
```

`damage()` asks the selected armor how much damage passes through and then wears it down. The
player subclass also applies its own armor encumbrance when calculating movement speed: Heavy feels
none of the armor speed penalty, Normal feels all of it, and Light feels 1.5 times the penalty.
