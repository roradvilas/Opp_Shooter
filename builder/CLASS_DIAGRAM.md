# Builder UML class diagram

The concrete abstract factories act as directors: each selects its product class, then calls the
same builder steps to assemble a `Player`. The combat and training builders give those steps
different meanings.

```mermaid
classDiagram
    direction LR

    class PlayerBuilder {
        <<interface>>
        +start(player) PlayerBuilder
        +addWeapon(type) PlayerBuilder
        +addArmor(type) PlayerBuilder
        +addMovement() PlayerBuilder
        +addAmmo() PlayerBuilder
        +build() Player
    }
    class CombatPlayerBuilder {
        -player Player
        +start(player) PlayerBuilder
        +addWeapon(type) PlayerBuilder
        +addArmor(type) PlayerBuilder
        +addMovement() PlayerBuilder
        +addAmmo() PlayerBuilder
        +build() Player
    }
    class TrainingPlayerBuilder {
        -player Player
        +start(player) PlayerBuilder
        +addWeapon(type) PlayerBuilder
        +addArmor(type) PlayerBuilder
        +addMovement() PlayerBuilder
        +addAmmo() PlayerBuilder
        +build() Player
    }

    class Player
    class CombatFactory
    class TrainingFactory
    class WeaponType
    class ArmorType

    PlayerBuilder <|.. CombatPlayerBuilder
    PlayerBuilder <|.. TrainingPlayerBuilder
    CombatPlayerBuilder --> Player : assembles
    TrainingPlayerBuilder --> Player : assembles
    CombatFactory ..> CombatPlayerBuilder : directs steps
    TrainingFactory ..> TrainingPlayerBuilder : directs steps
    PlayerBuilder ..> WeaponType : configures weapon
    PlayerBuilder ..> ArmorType : configures armor
```

Each factory creates a fresh builder for one player and runs `start`, `addWeapon`, `addArmor`,
`addMovement`, `addAmmo`, and `build`. `CombatPlayerBuilder` equips walking movement and 60 ammo;
`TrainingPlayerBuilder` equips sneaking movement, 999 ammo, and a logging weapon decorator.
