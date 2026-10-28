# Opp Shooter class diagram

This diagram includes the program's classes, interfaces, enums, records, and nested types across
packages. The `game` package contains the runtime, while the other packages implement the design
patterns used by it.

```mermaid
classDiagram
    direction LR

    class Player
    class HeavyPlayer
    class NormalPlayer
    class LightPlayer
    class Armor {
        <<interface>>
    }
    class NoArmor
    class KevlarArmor
    class PlatedArmor
    class PlayerSubject {
        <<interface>>
    }
    class PlayerObserver {
        <<interface>>
    }
    class PlayerEvent
    class PlayerEventType
    class PlayerType
    class ArmorType

    PlayerSubject <|.. Player
    Player <|-- HeavyPlayer
    Player <|-- NormalPlayer
    Player <|-- LightPlayer
    Player --> Armor : equipped armor
    Armor <|.. NoArmor
    Armor <|.. KevlarArmor
    Armor <|.. PlatedArmor
    Player ..> PlayerObserver : notifies
    Player ..> PlayerEvent : publishes
    Player --> PlayerType : class type
    ArmorType ..> Armor : creates
    PlayerEvent *-- PlayerEventType : type

    class MovementStrategy {
        <<interface>>
    }
    class Sneak
    class Walk
    class Sprint
    class Dash
    class MovementType
    class WeaponStrategy {
        <<interface>>
    }
    class WeaponType
    class Pistol
    class AssaultRifle
    class Shotgun
    class WeaponDecorator {
        <<abstract>>
    }
    class RapidFireDecorator
    class DamageBoostDecorator
    class LoggingWeaponDecorator
    class LaserGunAdapter
    class LegacyLaserGun
    class Bullet

    Player --> MovementStrategy : movement
    Player --> MovementType : selects
    MovementStrategy <|.. Sneak
    MovementStrategy <|.. Walk
    MovementStrategy <|.. Sprint
    MovementStrategy <|.. Dash
    MovementType ..> MovementStrategy : creates
    Player --> WeaponStrategy : equipped weapon
    Player --> WeaponType : equipped type
    WeaponStrategy <|.. Pistol
    WeaponStrategy <|.. AssaultRifle
    WeaponStrategy <|.. Shotgun
    WeaponStrategy <|.. WeaponDecorator
    WeaponDecorator <|-- RapidFireDecorator
    WeaponDecorator <|-- DamageBoostDecorator
    WeaponDecorator <|-- LoggingWeaponDecorator
    WeaponDecorator o--> WeaponStrategy : wraps
    WeaponStrategy <|.. LaserGunAdapter
    LaserGunAdapter --> LegacyLaserGun : adapts
    WeaponStrategy ..> Bullet : creates
    DamageBoostDecorator ..> Bullet : modifies
    WeaponType ..> WeaponStrategy : creates

    class WeaponFactory {
        <<abstract>>
    }
    class PistolFactory
    class AssaultRifleFactory
    class ShotgunFactory
    class LaserFactory
    WeaponFactory <|-- PistolFactory
    WeaponFactory <|-- AssaultRifleFactory
    WeaponFactory <|-- ShotgunFactory
    WeaponFactory <|-- LaserFactory
    WeaponFactory ..> WeaponStrategy : creates
    WeaponType ..> WeaponFactory : selects
    PistolFactory ..> Pistol : creates
    AssaultRifleFactory ..> AssaultRifle : creates
    ShotgunFactory ..> Shotgun : creates
    LaserFactory ..> LaserGunAdapter : creates

    class AbstractFactory {
        <<abstract>>
    }
    class CombatFactory
    class TrainingFactory
    class PlayerBuilder {
        <<interface>>
    }
    class CombatPlayerBuilder
    class TrainingPlayerBuilder
    AbstractFactory <|-- CombatFactory
    AbstractFactory <|-- TrainingFactory
    PlayerBuilder <|.. CombatPlayerBuilder
    PlayerBuilder <|.. TrainingPlayerBuilder
    CombatFactory ..> CombatPlayerBuilder : builds with
    TrainingFactory ..> TrainingPlayerBuilder : builds with
    AbstractFactory ..> Player : creates
    AbstractFactory ..> PlayerType : selects class
    CombatPlayerBuilder ..> Player : configures
    TrainingPlayerBuilder ..> Player : configures

    class CombatHeavyPlayer
    class CombatNormalPlayer
    class CombatLightPlayer
    class TrainingHeavyPlayer
    class TrainingNormalPlayer
    class TrainingLightPlayer
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

    class BulletModifiers
    class CopyMode
    Bullet *-- BulletModifiers : modifiers
    Bullet ..> CopyMode : copy mode

    class Command {
        <<interface>>
    }
    class MoveCommand
    class ShootCommand
    class SwitchMovementCommand
    class CommandInvoker
    Command <|.. MoveCommand
    Command <|.. ShootCommand
    Command <|.. SwitchMovementCommand
    CommandInvoker o--> Command : history
    MoveCommand --> Player : changes
    ShootCommand --> Player : fires for
    SwitchMovementCommand --> Player : changes movement

    class GameFacade
    class World
    class WorldGenerator
    class Loot
    class LootType
    class GameServer
    class BotController
    class ServerMain
    class ServerClient
    class Logger
    GameFacade --> World
    GameFacade --> WorldGenerator
    GameFacade --> AbstractFactory : creates players
    GameFacade --> CommandInvoker : per player
    GameFacade --> Logger
    GameFacade ..> Bullet : updates
    GameFacade ..> Loot : manages
    GameFacade ..> LootType
    GameServer --> GameFacade
    GameServer *-- ServerClient : connection handler
    BotController --> GameFacade
    ServerClient ..|> PlayerObserver : observes player
    ServerMain ..> GameServer : starts
    ServerMain ..> AbstractFactory : selects mode

    class ClientFacade
    class WebSocketClient
    class WebSocketServer
    class Protocol
    class Game
    class Input
    class PlayerInput
    class GameState
    class PlayerView
    class BulletView
    class ClientListener {
        <<interface>>
    }
    class WebSocketClientListener {
        <<interface>>
    }
    class SpawnRequest
    ClientFacade --> WebSocketClient : transport
    ClientFacade --> Protocol : encodes and decodes
    Game --> ClientFacade : uses
    ClientListener <|.. Game : receives updates
    ClientFacade ..> ClientListener : callbacks
    ClientFacade ..> PlayerEvent : delivers event
    ClientFacade ..> GameState : delivers state
    WebSocketClient *-- WebSocketClientListener : callbacks
    Game --> Input : handles controls
    Input ..> MovementType : selects
    Game ..> PlayerInput : builds from controls
    ClientFacade ..> PlayerInput : sends
    Game ..> GameState : renders
    GameState o--> PlayerView
    GameState o--> BulletView
    GameState o--> Loot
    GameServer --> WebSocketServer : serves connections
    GameServer --> Protocol : parses and encodes messages
    GameFacade --> Protocol : encodes state and events
    Game --> WorldGenerator : creates client map
    BotController --> WorldGenerator : creates shared map
    Protocol ..> SpawnRequest : parses spawn
    Protocol ..> PlayerInput : parses input
    Protocol ..> GameState : parses state
    Protocol ..> PlayerView : encodes and parses
    Protocol ..> BulletView : parses
    Protocol ..> PlayerType
    Protocol ..> WeaponType
    Protocol ..> ArmorType
    Protocol ..> MovementType
    Protocol ..> PlayerEvent
    Protocol ..> Player : encodes state
    Protocol ..> Bullet : encodes state
    Protocol ..> Loot : encodes state
    SpawnRequest --> PlayerType
    SpawnRequest --> WeaponType
    SpawnRequest --> ArmorType
    PlayerView --> PlayerType
    PlayerView --> WeaponType
    PlayerView --> ArmorType
    Loot --> LootType

    class Main
    Main ..> Game : launches
    class PrototypeDemo
    PrototypeDemo ..> Bullet : demonstrates copying
```

Solid inheritance arrows show class extension; dashed inheritance arrows show interface
implementation; composition and association arrows show fields or collaboration. `ServerClient`,
`ClientListener`, `WebSocketClientListener`, `SpawnRequest`, and `PlayerEventType` represent nested
types (`GameServer.Client`, `ClientFacade.Listener`, `WebSocketClient.Listener`,
`Protocol.SpawnRequest`, and `PlayerEvent.Type`).
