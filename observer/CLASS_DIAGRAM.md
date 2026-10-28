# Observer UML class diagram

`Player` publishes typed events to registered observers. The server's per-connection client
observes each spawned player and forwards those events to the remote UI.
See the [Observer sequence diagram](SEQUENCE_DIAGRAM.md) for the registration and event-delivery flow.

```mermaid
classDiagram
    direction LR

    class PlayerSubject {
        <<interface>>
        +addObserver(observer) void
        +removeObserver(observer) void
        +removeAllObservers() void
        +notifyObservers(event) void
    }
    class PlayerObserver {
        <<interface>>
        +onPlayerEvent(event) void
    }
    class Player {
        -observers List~PlayerObserver~
        +addObserver(observer) void
        +removeObserver(observer) void
        +removeAllObservers() void
        +notifyObservers(event) void
    }
    class PlayerEvent {
        <<record>>
        +playerId int
        +type Type
        +amount int
        +message() String
    }
    class PlayerEventType {
        <<enum>>
    }
    class GameServer
    class ServerClient
    class GameFacade
    class Protocol
    class WebSocketServer
    class WebSocketClient
    class ClientFacade
    class ClientListener {
        <<interface>>
    }
    class Game

    PlayerSubject <|.. Player
    PlayerObserver <|.. ServerClient
    Player o--> PlayerObserver : registered observers
    Player ..> PlayerEvent : publishes
    PlayerEvent *-- PlayerEventType : event type

    GameServer *-- ServerClient : connection handler
    GameServer --> GameFacade : spawns players
    GameServer ..> PlayerSubject : registers observer
    ServerClient --> WebSocketServer : sends event
    ServerClient ..> Protocol : encodes event
    WebSocketServer ..> WebSocketClient : network event message
    ClientFacade --> WebSocketClient : receives messages
    ClientFacade --> ClientListener : calls back
    ClientListener <|.. Game
    ClientFacade ..> PlayerEvent : creates from message
    Game ..> PlayerEvent : displays notification
    GameFacade ..> PlayerEvent : publishes undo event
```

`ServerClient`, `ClientListener`, and `PlayerEventType` stand for the nested types
`GameServer.Client`, `ClientFacade.Listener`, and `PlayerEvent.Type`. A player registers its
connection observer after spawning; the observer serializes each event, and the client facade
delivers the decoded event to `Game`.
