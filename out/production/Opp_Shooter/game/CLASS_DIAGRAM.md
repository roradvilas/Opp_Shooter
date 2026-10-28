# Game package UML class diagram

This diagram shows the runtime classes in `game` and the main cross-package types they use for
simulation, networking, input, and rendering.

```mermaid
classDiagram
    direction LR

    class Main
    class Game {
        +start() void
        +run() void
    }
    class Input
    class ServerMain
    class GameServer
    class ServerClient
    class BotController
    class WebSocketClient
    class WebSocketClientListener {
        <<interface>>
    }
    class WebSocketServer
    class WorldGenerator {
        +seed(seed) WorldGenerator
        +generate() World
    }
    class World
    class Loot
    class LootType

    class ClientFacade
    class ClientFacadeListener {
        <<interface>>
    }
    class GameFacade
    class Protocol
    class PlayerInput
    class GameState
    class PlayerView
    class BulletView
    class Player
    class PlayerObserver {
        <<interface>>
    }
    class PlayerEvent
    class AbstractFactory
    class CombatFactory
    class TrainingFactory
    class PlayerType
    class WeaponType
    class ArmorType
    class MovementType

    Main ..> Game : launches UI
    Game --> Input : reads controls
    Game --> WorldGenerator : builds client map
    Game --> World : draws map
    Game --> ClientFacade : connects and sends input
    Game ..|> ClientFacadeListener : receives callbacks
    Game ..> GameState : renders snapshots
    Game ..> PlayerView : renders players
    Game ..> BulletView : renders bullets
    Game ..> Loot : renders pickups
    Game ..> PlayerType : spawn selection
    Game ..> WeaponType : spawn selection
    Game ..> ArmorType : spawn selection

    ClientFacade --> WebSocketClient : transport
    ClientFacade --> Protocol : message encoding
    ClientFacade --> ClientFacadeListener : notifies
    WebSocketClient *-- WebSocketClientListener : transport callbacks

    ServerMain ..> GameServer : starts server
    ServerMain ..> GameFacade : configures simulation
    ServerMain ..> CombatFactory : selects combat mode
    ServerMain ..> TrainingFactory : selects training mode
    ServerMain ..> BotController : enables training bots
    GameServer --> GameFacade : processes player input and ticks
    GameServer --> WebSocketServer : serves connections
    GameServer --> Protocol : parses and encodes messages
    GameServer *-- ServerClient : connection handlers
    GameServer --> BotController : optional bots
    ServerClient --> WebSocketServer : reads and sends frames
    ServerClient ..|> PlayerObserver : observes player
    ServerClient --> PlayerInput : stores latest input
    ServerClient ..> PlayerEvent : sends player events

    BotController --> GameFacade : spawns bots and applies input
    BotController --> WorldGenerator : rebuilds seeded map
    BotController --> World : routes around walls
    BotController ..> Player : finds targets
    BotController ..> PlayerInput : builds bot input
    BotController ..> PlayerType : selects bot class
    BotController ..> WeaponType : selects bot weapon
    BotController ..> ArmorType : selects bot armor
    BotController ..> MovementType : selects bot movement

    WorldGenerator ..> World : creates
    World --> Loot : pickups
    Loot --> LootType : pickup type
    GameFacade --> AbstractFactory : creates players
    AbstractFactory <|-- CombatFactory
    AbstractFactory <|-- TrainingFactory
    GameFacade --> World : simulation map
```

`ServerClient`, `ClientFacadeListener`, and `WebSocketClientListener` represent the nested types
`GameServer.Client`, `ClientFacade.Listener`, and `WebSocketClient.Listener`. `Main` launches the
Swing client; `ServerMain` selects combat or training configuration before starting `GameServer`.
