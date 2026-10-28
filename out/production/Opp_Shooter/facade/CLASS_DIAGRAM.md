# Facade UML class diagram

The program has a server-side facade for game simulation and a client-side facade for the network
connection. Each gives its callers a smaller API over several subsystems.

```mermaid
classDiagram
    direction LR

    class GameFacade {
        +spawn(id, type, weapon, armor) Player
        +applyInput(id, input, now) void
        +tick(now) List~Integer~
        +stateMessage() String
        +remove(id) void
    }
    class GameServer
    class BotController
    class World
    class WorldGenerator
    class AbstractFactory
    class CommandInvoker
    class Player
    class Bullet
    class Loot
    class Logger
    class Protocol

    GameServer --> GameFacade : server calls
    BotController --> GameFacade : bot calls
    GameFacade --> World : map and loot
    GameFacade --> WorldGenerator : creates world
    GameFacade --> AbstractFactory : creates players
    GameFacade o--> Player : active players
    GameFacade o--> CommandInvoker : history per player
    GameFacade o--> Bullet : active bullets
    GameFacade --> Logger : logging
    GameFacade ..> Protocol : encodes state
    GameFacade ..> Loot : manages pickups

    class ClientFacade {
        +connect() void
        +close() void
        +spawn(type, weapon, armor) void
        +sendInput(input) void
    }
    class ClientListener {
        <<interface>>
        +onWelcome(playerId, worldSeed) void
        +onSpawned() void
        +onDied() void
        +onEvent(event) void
        +onState(state) void
        +onDisconnected() void
    }
    class Game
    class WebSocketClient
    class WebSocketServer
    class GameState
    class PlayerInput
    class PlayerEvent
    class PlayerView
    class BulletView
    class SpawnRequest
    class MovementType
    class PlayerType
    class WeaponType
    class ArmorType

    Game --> ClientFacade : UI uses
    ClientListener <|.. Game : UI receives callbacks
    ClientFacade --> ClientListener : notifies
    ClientFacade --> WebSocketClient : hides transport
    ClientFacade --> Protocol : encodes and decodes
    ClientFacade ..> PlayerInput : sends input
    ClientFacade ..> GameState : delivers decoded state
    ClientFacade ..> PlayerEvent : delivers events
    GameServer --> WebSocketServer : server transport
    GameServer --> Protocol : parses and encodes messages

    Protocol ..> SpawnRequest : parses spawn requests
    Protocol ..> PlayerInput : parses input
    Protocol ..> GameState : parses state
    Protocol ..> PlayerView : maps player data
    Protocol ..> BulletView : maps bullet data
    Protocol ..> PlayerEvent : maps events
    Protocol ..> Player : encodes state
    Protocol ..> Bullet : encodes state
    Protocol ..> Loot : encodes state
    SpawnRequest --> PlayerType
    SpawnRequest --> WeaponType
    SpawnRequest --> ArmorType
    PlayerInput --> MovementType

    GameState o--> PlayerView
    GameState o--> BulletView
    GameState o--> Loot
    PlayerView --> PlayerType
    PlayerView --> WeaponType
    PlayerView --> ArmorType
```

`GameServer` and `BotController` use the same `GameFacade` operations. On the client, `Game` uses
`ClientFacade` and receives typed callbacks, so it does not manage WebSocket transport or protocol
parsing itself. `Protocol` centralizes the wire formats used on both sides.
