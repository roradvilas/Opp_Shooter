# Game design patterns

Every pattern lives in its own Java package. Non-pattern code (map, loot, Swing client, WebSocket
code, `GameServer`, `BotController`) is in package `game`. The repository has one demo class,
`prototype.PrototypeDemo`; the other outputs quoted below were captured from throwaway demo runs of the
classes described.

| Requirement | Package | Implementation and evidence |
| --- | --- | --- |
| a. Singleton, thread-safe | `singleton` | `Logger`; a 200-thread run saw one instance (see below). |
| b. Factory, 3+ classes | `factory` | abstract `WeaponFactory` (factory method `createWeapon()`) → `PistolFactory`, `AssaultRifleFactory`, `ShotgunFactory`, `LaserFactory`. |
| c. Abstract Factory, 2+ factories × 3 classes | `abstractfactory` | abstract class `AbstractFactory`; `CombatFactory` and `TrainingFactory` each produce 3 player classes. |
| d. Strategy, 4+ classes | `strategy` | `Sneak`, `Walk`, `Sprint`, `Dash` (`MovementStrategy`); `Pistol`, `AssaultRifle`, `Shotgun` (`WeaponStrategy`). |
| e. Observer, sequence diagram | `observer` | `PlayerSubject`, `PlayerObserver`, `PlayerEvent`; diagram below. |
| f. Builder, 2+ concrete builders | `builder` | `PlayerBuilder`; `CombatPlayerBuilder`, `TrainingPlayerBuilder`; the concrete factories use them as clients. |
| g. Adapter, different method counts | `adapter` | `LaserGunAdapter` (4-method `WeaponStrategy`) wraps `LegacyLaserGun` (6 methods). |
| h. Prototype, deep vs shallow, addresses | `prototype` | `Bullet`, `BulletModifiers`, `CopyMode`; `PrototypeDemo` prints the addresses (identity hashes) of the copies; output below. |
| i. Decorator, 3+ levels | `decorator` | `Logging(DamageBoost(RapidFire(Pistol)))`. |
| j. Command with `undo()` | `command` | `MoveCommand`, `ShootCommand`, `SwitchMovementCommand`; `CommandInvoker`. |
| k. Facade, 2+ clients, 3+ subsystems | `facade` | `GameFacade` (server side) with clients `game.GameServer` and `game.BotController`; `ClientFacade` (client side) hides the connection; details below. |
| l. Bridge, 2+ abstractions, 2+ implementations | `bridge` | `Player` → `HeavyPlayer`/`NormalPlayer`/`LightPlayer`; `Armor` → `NoArmor`/`KevlarArmor`/`PlatedArmor`. |

`Player`, `PlayerType` and the three player classes are in `bridge` because they are the Bridge
abstraction. `Bullet` is in `prototype`. State they expose (`Player.health`, `Bullet.damage`, ...)
is public so the other packages can use it.

## Singleton

`Logger` is created by a `static final` field initializer. The JVM runs class initialization exactly
once under a lock, so no thread can see two instances or a half-built one; `log` is `synchronized`
so concurrent lines never interleave. A run that started 200 threads behind a latch, released them
together, and checked the result gave:

```text
Threads released simultaneously: 200
Distinct Logger instances seen:  1 (identity 4af0c6a9)
Log lines written / intact:      200 / 200
PASS: one instance, no interleaved output
```

## Prototype

`Bullet implements Cloneable`; its nested `BulletModifiers` (`damageBonus`) is the
reference field whose copying differs between modes.

- `shallowCopy()` is `super.clone()`: the copy **shares** the `BulletModifiers`.
- `deepCopy()` is `shallowCopy()` plus `modifiers.copy()`: the copy owns its own.
- `copy(id, ownerId, x, y, angle)` uses the static, runtime-switchable
  `Bullet.getCopyMode()` / `setCopyMode()` (default `DEEP`), then sets id, owner, position,
  `alive = true`, and velocity from `angle × hypot(vx, vy)`.
- `effectiveDamage()` = `damage + modifiers.getDamageBonus()`; the server applies it on hit.

### Demo: comparing shallow and deep copies

`prototype.PrototypeDemo` (the only demo class in the repository) copies one `Bullet` with `shallowCopy()`,
`deepCopy()` and `copy()`, prints the "address" of every bullet and of its nested `BulletModifiers`, then
changes the shallow copy's `BulletModifiers` and prints again:

```bash
javac -d out prototype/*.java game/World.java game/Loot.java game/LootType.java   # or compile everything
java -cp out prototype.PrototypeDemo deep      # Bullet.copy() deep-copies (default)
java -cp out prototype.PrototypeDemo shallow   # Bullet.copy() shallow-copies
```

Java does not expose real memory addresses, so the identity hash code (`System.identityHashCode`, shown as hex)
stands in for the address: the same value means the same object. The values differ from run to run, but the
equalities do not. Captured output, `deep` mode (original has damage 25 + bonus 5 = 30):

```text
Copy mode used by Bullet.copy(): DEEP
-- before mutation
original  bullet=0x14dad5dc modifiers=0x0c387f44 effectiveDamage=30
shallow   bullet=0x232204a1 modifiers=0x0c387f44 effectiveDamage=30
deep      bullet=0x4aa298b7 modifiers=0x7d4991ad effectiveDamage=30
copy()    bullet=0x28d93b30 modifiers=0x1b6d3586 effectiveDamage=30
-- after shallow.modifiers().setDamageBonus(99)
original  bullet=0x14dad5dc modifiers=0x0c387f44 effectiveDamage=124
shallow   bullet=0x232204a1 modifiers=0x0c387f44 effectiveDamage=124
deep      bullet=0x4aa298b7 modifiers=0x7d4991ad effectiveDamage=30
copy()    bullet=0x28d93b30 modifiers=0x1b6d3586 effectiveDamage=30
-- shares the original's BulletModifiers object?
shallow: true
deep:    false
copy():  false
```

Same program in `shallow` mode (only the `copy()` rows differ from the run above):

```text
Copy mode used by Bullet.copy(): SHALLOW
-- before mutation
original  bullet=0x14dad5dc modifiers=0x0c387f44 effectiveDamage=30
shallow   bullet=0x232204a1 modifiers=0x0c387f44 effectiveDamage=30
deep      bullet=0x4aa298b7 modifiers=0x7d4991ad effectiveDamage=30
copy()    bullet=0x28d93b30 modifiers=0x0c387f44 effectiveDamage=30
-- after shallow.modifiers().setDamageBonus(99)
original  bullet=0x14dad5dc modifiers=0x0c387f44 effectiveDamage=124
shallow   bullet=0x232204a1 modifiers=0x0c387f44 effectiveDamage=124
deep      bullet=0x4aa298b7 modifiers=0x7d4991ad effectiveDamage=30
copy()    bullet=0x28d93b30 modifiers=0x0c387f44 effectiveDamage=124
-- shares the original's BulletModifiers object?
shallow: true
deep:    false
copy():  true
```

What the numbers show:

| | Bullet object | `BulletModifiers` object | Objects created per copy | Mutating the copy's modifiers |
| --- | --- | --- | --- | --- |
| shallow copy | new address | **same address as the original** | 1 | changes the original too (30 → 124) |
| deep copy | new address | **new address** | 2 | original unchanged (30) |

Every copy gets a new `Bullet` address. Only the nested object tells the modes apart: a shallow copy
copies the reference (one shared `BulletModifiers`), a deep copy copies the object behind it. In the game
this matters because a weapon owns one prototype and every shot is a copy of it. With `DEEP` (the default)
`DamageBoostDecorator` can set a bonus on a fired bullet without touching the weapon's prototype; with
`SHALLOW` the same call would raise the damage of every later bullet from that weapon.

**Changing the implementation (defense cheat sheet).** Everything is in `prototype`:
- switch the mode for the game or the demo: `Bullet.setCopyMode(CopyMode.SHALLOW)` or the demo argument;
- add another nested mutable field: put it in `BulletModifiers`, copy it in `BulletModifiers.copy()` (that is what
  makes `deepCopy()` deep), and print it with `address(...)` in `PrototypeDemo`;
- make `copy()` always deep or always shallow: change the one `copyMode == CopyMode.SHALLOW ? ... : ...` line in `Bullet.copy`;
- the copy's identity (id, owner, position, velocity, `alive`) is set in `Bullet.copy`, not in the two copy methods.

## Bridge

Abstraction: `Player` holds an `Armor` (`getArmor` / `setArmor`, default `NoArmor`) and builds two
behaviours from its primitives. `damage()` applies `armor.absorb(amount)`; `effectiveSpeed()` is
`speed * (1 - armor.speedPenalty() * armorEncumbrance())`. Refined abstractions: `HeavyPlayer`,
`NormalPlayer`, `LightPlayer`; each overrides `armorEncumbrance()` to decide how much of the armor's
speed penalty it feels (Heavy 0.0, Normal 1.0, Light 1.5). Implementors: `NoArmor` (100% damage passes,
no slowdown), `KevlarArmor` (75%, -10% speed, durability 80), `PlatedArmor` (50%, -25% speed,
durability 120); damage rounds up, so a hit never becomes 0. Both dimensions affect the outcome and neither can be folded into the other
(a 40-point hit, and effective speed in pixels per tick):

```text
              None                     Kevlar                   Plated
HeavyPlayer   40 (80/120)   2.56       30 (90/120)   2.56       20 (100/120)  2.56
NormalPlayer  40 (60/100)   3.20       30 (70/100)   2.88       20 (80/100)   2.40
LightPlayer   40 (40/80)    3.84       30 (50/80)    3.26       20 (60/80)    2.40
```

Cells are damage taken (HP left / max HP) and speed (while the armor is intact). Armor is a trade-off that depends on the class: a
Heavy gets the protection for free, a Normal pays the full slowdown, and a Light in plated armor loses its
speed advantage entirely.

Durability: `Armor` also exposes `onHit(damage)`, `durability()` and `maxDurability()`. `Player.damage()`
asks the armor to `absorb` the hit, then calls `armor.onHit(damage)`, which wears the armor down by the full
damage of the hit (each concrete armor keeps its own durability counter). Once durability reaches 0 the armor is broken: it stops absorbing damage and stops slowing
its wearer, and the player gets an `ARMOR_BROKEN` event ("Armor broken!"). A 20-damage hit sequence on a
Normal player in Kevlar: 15, 15, 15, 15 damage taken (durability 60, 40, 20, 0), then the armor breaks and
the following hits do the full 20. `NoArmor` has no durability.

The armor is chosen on the spawn and death screen (`ArmorType`: `NONE`, `KEVLAR`, `PLATED`) and travels in
`SPAWN|TYPE|WEAPON|ARMOR`. The server sends `armor,durability,maxDurability` for every player in `STATE`, and the
client shows them as a durability bar above each player and as an "Armor: Kevlar 35/80" line with a bar in the HUD.

**Bridge vs Strategy vs Adapter**

| | Bridge | Strategy | Adapter |
| --- | --- | --- | --- |
| Intent | Split an abstraction from its implementation so **both hierarchies** grow independently (3 player classes × 3 armors, not 9 subclasses). | Make **one algorithm** of a context swappable at runtime. | Make an **existing, incompatible** class usable through the interface a client expects. |
| Structure | Abstraction (`Player`) holds an implementor (`Armor`); the abstraction itself has refined subclasses. | Context holds a strategy; the context has no hierarchy of its own that varies with the strategy. | Adapter implements the target and wraps the adaptee. |
| When decided | Up front, at design time. | Up front, to allow runtime swapping. | After the fact, because the adaptee already exists and cannot change. |
| Here | `Player`/`Armor`: the armor is chosen by the builder; class and armor vary separately and combine (damage and speed). | `Player`/`MovementStrategy`: movement is swapped whenever the client's key changes. | `LaserGunAdapter`/`LegacyLaserGun`: a legacy 6-method API presented as a 4-method `WeaponStrategy`. |

In short: Strategy swaps behaviour behind one interface, Adapter converts one interface into another,
and Bridge keeps two independently varying dimensions from multiplying into a class explosion.

## Decorator

`WeaponStrategy` is the component; `WeaponDecorator` holds `protected final WeaponStrategy
wrappee` and delegates all four methods. `LoggingWeaponDecorator` logs through
`Logger.getInstance()` before and after `fire`; `RapidFireDecorator` halves
`shootingDelay()`; `DamageBoostDecorator` sets a +10 damage bonus in the `BulletModifiers` of every bullet it returns
(`Bullet.effectiveDamage()` = damage + bonus), so the bullet's nested object carries real state.
Wrapping a `Pistol` in three decorators, one level at a time, gave:

```text
level  weapon                                              delay  damage
0      Pistol                                              325 ms    25
1      RapidFire(Pistol)                                   162 ms    25
2      DamageBoost(RapidFire(Pistol))                      162 ms    35
3      Logging(DamageBoost(RapidFire(Pistol)))             162 ms    35
```

## Abstract Factory

```java
abstract class AbstractFactory {
    abstract Player createHeavy(int id, double x, double y, WeaponType w, ArmorType a);
    abstract Player createNormal(int id, double x, double y, WeaponType w, ArmorType a);
    abstract Player createLight(int id, double x, double y, WeaponType w, ArmorType a);
    Player create(PlayerType t, int id, double x, double y, WeaponType w, ArmorType a) { /* switch on t */ }
}
```

| Factory | Heavy | Normal | Light | Behaviour |
| --- | --- | --- | --- | --- |
| `CombatFactory` | `CombatHeavyPlayer` | `CombatNormalPlayer` | `CombatLightPlayer` | same as the base classes |
| `TrainingFactory` | `TrainingHeavyPlayer` | `TrainingNormalPlayer` | `TrainingLightPlayer` | `damage()` never takes health below 1 |

## Builder

`PlayerBuilder` steps: `start(Player)`, `addWeapon(WeaponType)`, `addArmor(ArmorType)`,
`addMovement()`, `addAmmo()`, `build()`. The first five return the builder; `build()` returns
the finished `Player`. `CombatFactory` and `TrainingFactory` each use their own builder inside every
`create*` method (a fresh builder per player) to equip the player they create. The builders implement every step differently:

| Step | `CombatPlayerBuilder` | `TrainingPlayerBuilder` |
| --- | --- | --- |
| weapon | plain weapon from `WeaponFactory` | `LoggingWeaponDecorator(weapon)` |
| armor | the selected `ArmorType` | the selected `ArmorType` (same) |
| movement | `Walk` | `Sneak` |
| ammo | 60 | 999 |

```java
// inside CombatFactory.createHeavy(id, x, y, w, a)
Player p = new CombatPlayerBuilder().start(new CombatHeavyPlayer(id, x, y))
        .addWeapon(w).addArmor(a).addMovement().addAmmo().build();
```

## Adapter

Target `WeaponStrategy`; adaptee `LegacyLaserGun` with an incompatible API;
adapter `LaserGunAdapter`. `LaserFactory` returns
`new LaserGunAdapter(new LegacyLaserGun())` for `WeaponType.LASER`.

| `WeaponStrategy` (target, 4 methods) | `LegacyLaserGun` (adaptee, 6 methods) | Adapter mapping |
| --- | --- | --- |
| `shootingDelay()` | `rechargeTimeMs()` (400) | `(int) rechargeTimeMs()` |
| `bulletCount()` | – | constant 1 |
| `bulletPrototype()` | `beamSpeed()` (18), `beamDamage()` (35) | `Bullet` built from both |
| `fire(player, angle, ids)` | `chargeCapacitor()`, `isCharged()`, `dischargeCapacitor()` | charge; if charged, discharge and return 1 bullet |

## Command

`Command { String execute(); String undo(); }`, receiver `Player`.

| Command | `execute` | `undo` |
| --- | --- | --- |
| `MoveCommand` | saves x, y; `player.move(...)` | restores x, y |
| `ShootCommand` | saves ammo, lastShot; fires into `out` | restores both; fired bullets get `alive = false` |
| `SwitchMovementCommand` | saves previous type + strategy; `setMovement` | re-equips the saved pair |

`CommandInvoker` keeps a `Deque<Command>` capped at 100: `run` executes and pushes, `undo`
pops and undoes; `undo(count)` repeats that for up to `count` commands, newest first, and returns what
each undo reported. `GameFacade` gives every spawned player its own invoker. Each tick it
runs a `SwitchMovementCommand` only when the requested movement differs from the player's current
one, a `MoveCommand` while a direction key is held, and a `ShootCommand` while the fire button is
held and the weapon is ready. Running each command and then undoing it gave:

```text
  [start                     ] position=(200.0, 200.0) ammo=60 ... liveBullets=0
  [run  MoveCommand          ] position=(203.2, 200.0) ammo=60 ... liveBullets=0
  [undo                      ] position=(200.0, 200.0) ammo=60 ... liveBullets=0
  [run  ShootCommand         ] position=(200.0, 200.0) ammo=59 ... liveBullets=1
  [undo                      ] position=(200.0, 200.0) ammo=60 ... liveBullets=0
```

In training mode `undo()` is also used for server-side move validation. `GameFacade(factory,
invalidMoveChance)` gives every executed `MoveCommand` a small chance (0.5%, set in `ServerMain`) of being
flagged as an invalid packet. The facade then calls the player's `CommandInvoker.undo(20)`, which rolls back
the player's last 20 commands (moves, shots and movement switches, newest first; fewer if the history is
shorter), logs a summary, and publishes an `UNDO_COMPLETED` `PlayerEvent` whose amount is the number of
commands undone. The client's observer forwards it as `EVENT|UNDO_COMPLETED|20`, and the HUD shows
"Invalid packet: undid last 20 commands" for three seconds. With 30 moves in the history, `undo(20)` put the
player back at its position after move 10 and left 10 commands in the history. Only state the commands
saved is restored: bullets that already hit someone and loot already picked up stay as they are.

## Facade

Two facades simplify the client-server boundary, one per side. Neither side builds or splits
protocol strings itself: both go through `facade.Protocol`, which holds every message format.

### Server side: `GameFacade`

`GameFacade(AbstractFactory)` hides `World`, `WorldGenerator`, the factory (which builds the players),
per-player `CommandInvoker`s, bullets, loot and `Logger`:

| Method | Does |
| --- | --- |
| `spawn(id, PlayerType, WeaponType, ArmorType)` | finds a free position, creates via the factory, equips via the builder |
| `applyInput(id, PlayerInput, now)` | one tick of a player's held input, as commands; then loot pickup |
| `tick(now)` | timed loot spawning, bullet movement and hits; returns ids killed this tick |
| `stateMessage()` | the `STATE\|...` protocol line (encoded by `Protocol.state`) |
| `remove(id)` | drops a player on death or disconnect |
| `players()`, `hasPlayer(id)`, `worldSeed()` | read-only queries |

**Clients (2):** `game.GameServer` (feeds it network input) and `game.BotController` (feeds it
bot input). Both use the same methods.

**Subsystems (4):** `game.World` (map, walls, loot), `abstractfactory.AbstractFactory` (builds players with
`builder.PlayerBuilder`), `command.CommandInvoker` (one per player), and `singleton.Logger`; bullets are
kept inside the facade.

`GameServer` keeps only sockets and threads.

### Client side: `ClientFacade`

`new ClientFacade(host, port, listener)` hides the `WebSocketClient` transport (handshake, frames,
reader thread) and `Protocol` decoding. The Swing `Game` calls a few methods and implements one
listener:

| Call | Does |
| --- | --- |
| `connect()` / `close()` | opens or closes the WebSocket connection |
| `spawn(PlayerType, WeaponType, ArmorType)` | sends `SPAWN\|TYPE\|WEAPON\|ARMOR` |
| `sendInput(PlayerInput)` | sends `INPUT\|...` |

| `ClientFacade.Listener` callback | Message |
| --- | --- |
| `onWelcome(playerId, worldSeed)` | `WELCOME` |
| `onSpawned()` / `onDied()` | `SPAWNED` / `DIED` |
| `onEvent(PlayerEvent)` | `EVENT` |
| `onState(GameState)` | `STATE`, decoded into `PlayerView`, `BulletView` and `Loot` records |
| `onDisconnected()` | connection closed |

`Game` no longer contains a single protocol string.

### Training mode

`java game.ServerMain --training [port]` builds the server facade with `TrainingFactory` +
`TrainingPlayerBuilder` (players cannot die, 999 ammo, logged shots, Sneak by default), flags about 0.5% of
moves as invalid packets and undoes them (see Command), and starts a `BotController` with 2 bots. Without the flag it uses `CombatFactory` + `CombatPlayerBuilder` and no bots.

The bots have the reserved ids -1 and -2 and are ordinary players inside the facade. Every tick each bot
finds the closest living human, computes a breadth-first route over the map (rebuilt from the shared
seed, like a client does), and walks toward it until it is 60 px away. If no human is connected they idle.
Bots do not shoot.

## Observer

`PlayerSubject` declares `addObserver`, `removeObserver` and `notifyObservers(PlayerEvent)`;
`Player` implements it. Observers register with `addObserver` and unsubscribe with
`removeObserver`. State changes create typed `PlayerEvent` values; the server's
`GameServer.Client` implements `PlayerObserver` and forwards them over WebSocket, and
`ClientFacade` delivers them to `Game.onEvent` as HUD notifications.

```mermaid
sequenceDiagram
    participant Server as GameServer
    participant Facade as GameFacade
    participant Subject as Player (PlayerSubject)
    participant Observer as Client (PlayerObserver)
    participant Socket as WebSocketServer
    participant CF as ClientFacade
    participant HUD as Game (UI)
    Server->>Facade: spawn(id, type, weapon, armor)
    Facade-->>Server: Player
    Server->>Subject: addObserver(client)
    Server->>Facade: tick(now)
    Facade->>Subject: damage(bullet.effectiveDamage())
    Subject->>Subject: armor.absorb(amount), reduce health
    Subject->>Subject: notifyObservers(DAMAGED event)
    Subject->>Observer: onPlayerEvent(DAMAGED, actual damage)
    Observer->>Socket: sendText(EVENT|DAMAGED|amount)
    Socket-->>CF: onText(EVENT|DAMAGED|amount)
    CF->>HUD: onEvent(DAMAGED)
    HUD->>HUD: show hit notification
    opt health reaches zero
        Subject->>Observer: onPlayerEvent(DIED, 0)
        Observer->>Socket: sendText(EVENT|DIED|0)
        Socket-->>CF: onText(EVENT|DIED|0)
        CF->>HUD: onEvent(DIED)
        HUD->>HUD: show death notification
        Facade->>Facade: remove(player)
        Facade-->>Server: killed ids
        Server->>Observer: send(DIED)
        Observer->>Socket: sendText(DIED)
        Socket-->>CF: onText(DIED)
        CF->>HUD: onDied()
        HUD->>HUD: show respawn screen
    end
```

Damage events precede death events. The separate `DIED` protocol message controls the
respawn screen. Loot, health pickup, and empty ammo also publish events. The observer list
is a `CopyOnWriteArrayList`, and callbacks run synchronously on the publisher's thread.

## Class diagram

```mermaid
classDiagram
    WeaponFactory <|-- PistolFactory
    WeaponFactory <|-- AssaultRifleFactory
    WeaponFactory <|-- ShotgunFactory
    WeaponFactory <|-- LaserFactory
    AbstractFactory <|-- CombatFactory
    AbstractFactory <|-- TrainingFactory
    HeavyPlayer <|-- CombatHeavyPlayer
    HeavyPlayer <|-- TrainingHeavyPlayer
    NormalPlayer <|-- CombatNormalPlayer
    NormalPlayer <|-- TrainingNormalPlayer
    LightPlayer <|-- CombatLightPlayer
    LightPlayer <|-- TrainingLightPlayer
    Player <|-- HeavyPlayer
    Player <|-- NormalPlayer
    Player <|-- LightPlayer
    Player --> Armor : bridge
    Armor <|.. NoArmor
    Armor <|.. KevlarArmor
    Armor <|.. PlatedArmor
    PlayerSubject <|.. Player
    Player --> MovementStrategy
    Player --> WeaponStrategy
    MovementStrategy <|.. Sneak
    MovementStrategy <|.. Walk
    MovementStrategy <|.. Sprint
    MovementStrategy <|.. Dash
    WeaponStrategy <|.. Pistol
    WeaponStrategy <|.. AssaultRifle
    WeaponStrategy <|.. Shotgun
    WeaponStrategy <|.. LaserGunAdapter
    LaserGunAdapter --> LegacyLaserGun
    WeaponStrategy <|.. WeaponDecorator
    WeaponDecorator o-- WeaponStrategy : wrappee
    WeaponDecorator <|-- LoggingWeaponDecorator
    WeaponDecorator <|-- RapidFireDecorator
    WeaponDecorator <|-- DamageBoostDecorator
    PlayerBuilder <|.. CombatPlayerBuilder
    PlayerBuilder <|.. TrainingPlayerBuilder
    CombatFactory --> CombatPlayerBuilder : builds with
    TrainingFactory --> TrainingPlayerBuilder : builds with
    Command <|.. MoveCommand
    Command <|.. ShootCommand
    Command <|.. SwitchMovementCommand
    CommandInvoker o-- Command : history
    GameFacade --> AbstractFactory
    GameFacade --> CommandInvoker
    GameServer --> GameFacade
    BotController --> GameFacade
    GameFacade --> Protocol
    Game --> ClientFacade
    ClientFacade --> Protocol
    ClientFacade --> WebSocketClient
```

## Running it

Compile everything into one output directory (Git Bash, from the project directory):

```bash
javac --release 17 -d out $(find . -name "*.java" -not -path "./out/*")
java -cp out game.ServerMain --training   # or without the flag for combat mode
java -cp out game.Main                    # one or more clients
```

In training mode two bots (ids -1 and -2) walk toward the closest player around walls and stop next to it.
