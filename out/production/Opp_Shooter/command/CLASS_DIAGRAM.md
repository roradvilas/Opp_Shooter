# Command UML class diagram

Each concrete command stores the state it needs to execute and undo one action on a `Player`.
`CommandInvoker` runs commands and keeps a bounded history for undo.

```mermaid
classDiagram
    direction LR

    class Command {
        <<interface>>
        +execute() String
        +undo() String
    }
    class MoveCommand {
        -player Player
        -world World
        -oldX double
        -oldY double
        +execute() String
        +undo() String
    }
    class ShootCommand {
        -player Player
        -out List~Bullet~
        -oldAmmo int
        -oldLastShot long
        +execute() String
        +undo() String
    }
    class SwitchMovementCommand {
        -player Player
        -oldType MovementType
        -oldStrategy MovementStrategy
        +execute() String
        +undo() String
    }
    class CommandInvoker {
        -history Deque~Command~
        +run(command) String
        +undo() String
        +undo(count) List~String~
    }
    class Player
    class World
    class Bullet
    class MovementType
    class MovementStrategy {
        <<interface>>
    }
    class GameFacade

    Command <|.. MoveCommand
    Command <|.. ShootCommand
    Command <|.. SwitchMovementCommand
    CommandInvoker o--> Command : history (up to 100)

    MoveCommand --> Player : receiver
    MoveCommand --> World : movement collision checks
    ShootCommand --> Player : receiver
    ShootCommand ..> Bullet : creates and cancels
    SwitchMovementCommand --> Player : receiver
    SwitchMovementCommand --> MovementType : target type
    SwitchMovementCommand --> MovementStrategy : saves previous strategy

    GameFacade *--> CommandInvoker : one per player
    GameFacade ..> MoveCommand : creates
    GameFacade ..> ShootCommand : creates
    GameFacade ..> SwitchMovementCommand : creates
```

Undo runs newest first. `MoveCommand` restores position, `ShootCommand` restores ammo and shot time
and marks its bullets dead, and `SwitchMovementCommand` re-equips the previous movement type and
strategy.
