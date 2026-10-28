# Hell Is Others Lite — raw WebSocket PvP

This version keeps the original Java/Swing client, but turns the game into a
server-authoritative PvP multiplayer game.

## Run

Requires JDK 17+.

Compile:
```bash
javac --release 17 -d out $(find . -name "*.java" -not -path "./out/*")
```

Start the server:
```bash
java -cp out game.ServerMain                  # combat mode (default), port 8080
java -cp out game.ServerMain --training       # training mode: players cannot die, 999 ammo, logged shots, random invalid-packet rollbacks, 2 bots
java -cp out game.ServerMain --training 9000  # the flag and the port can be combined
```

Then, in two or more terminals:
```bash
java -cp out game.Main
```

By default clients connect to `localhost:8080`.

The client opens an in-game spawn screen with class, weapon and armor selectors (the same screen is
shown again after death, so all three can be changed before respawning). Heavy has
120 HP and 80% speed (2.56 per tick); Normal has 100 HP and 100% speed (3.2);
Light has 80 HP and 120% speed (3.84). All have the same ammo and collision radius. Armor also slows
its wearer: Kevlar costs 10% speed and Plated 25%, scaled by class (Heavy feels none of it, Normal all of it,
Light 1.5 times it). Armor also has durability (Kevlar 80, Plated 120): every hit wears it down by the
damage dealt, and once it reaches 0 the armor breaks and no longer protects or slows. The HUD shows
"Armor: Kevlar 35/80" with a bar, and every player has a small durability bar above their health bar.
The server constructs selected loadouts inside `GameFacade`: a concrete
`AbstractFactory` (`CombatFactory` or `TrainingFactory`) creates the player and uses a
`PlayerBuilder` (`CombatPlayerBuilder` or `TrainingPlayerBuilder`) internally to equip it.

In training mode the server also randomly flags a move as an invalid packet and undoes the player's last 20 commands
(the HUD shows "Invalid packet: undid last 20 commands"). Two server-side bots always walk toward the closest player (routing around
walls) and stop next to them; they do not shoot.

Move with WASD. Hold Ctrl to Sneak (50% of class speed), Shift to Sprint (150%),
or Space to Dash (300% for 150 ms, with a one-second cooldown). Walk uses the
class's baseline speed. Dash cooldown persists when switching movement modes.

Choose a loadout and click Spawn to enter the world. On death, your body disappears
and the same screen returns with a Respawn button. You can change class and weapon
before each respawn; health, ammo, and collected loot reset. Aiming follows the
mouse and is synchronized to other clients. Picked-up loot disappears for everyone.

The map starts with no loot. The server spawns one pickup every five seconds on
an open floor tile more than two tiles (80 pixels) from every existing pickup,
randomly choosing one of four types with equal probability. This circular exclusion
radius applies to every type and includes positions exactly two tiles away.
Gold boxes give 5 ammo; green boxes with a white cross restore up to 25 HP,
capped at the selected class's maximum health. Blue boxes are a rapid-fire power-up
(`RapidFireDecorator`, half the shooting delay) and red boxes a damage boost
(`DamageBoostDecorator`, +10 damage per bullet): collecting one wraps the player's
current weapon at runtime. Power-ups last until death or a weapon change, since a
respawn builds a new player. They do not stack: a player already wearing a power-up
leaves another of the same kind on the ground (different kinds can be combined), as
full-health players do with health pickups. Every collected pickup increases the loot
counter and shows a notification. Loot remains until collected; spawning is skipped
when no floor tile satisfies the spacing rule.

Weapons implement `WeaponStrategy` and share the server's bullet ID counter and
the player's ammo pool. Shooting speed means fire rate; all bullets retain the
original velocity and damage:

| Weapon | Bullets per shot | Fire rate | Delay | Ammo per shot |
| --- | --- | --- | --- | --- |
| Assault Rifle | 1 | 100% | 130 ms | 1 |
| Pistol | 1 | 40% | 325 ms | 1 |
| Shotgun | 3 | 20% | 650 ms | 3 |
| Laser (adapted `LegacyLaserGun`) | 1 | 32.5% | 400 ms | 1 |

The shotgun fires a center pellet and two side pellets, each offset randomly by
3–15 degrees from the aim direction. It requires three rounds to fire a full shot.

For another machine, change `SERVER_HOST` in `Game.java` to the server's IP.

## Networking

There is deliberately **no WebSocket framework/library**. `WebSocketServer`
implements the RFC 6455 opening handshake and basic text-frame parsing/writing
directly on `ServerSocket`/`Socket`.

The client talks to the server only through `ClientFacade` (connect, spawn, sendInput plus a
listener with typed callbacks) and the server is driven through `GameFacade`; both share
`facade.Protocol`, the single place where messages are encoded and decoded.

The server owns authoritative player positions, shooting, bullets, HP, ammo and
loot. Clients send input snapshots; the server simulates the world and broadcasts
snapshots.

Console messages use the singleton `Logger` via `Logger.getInstance().log(text)`.

Protocol:
- client -> server: `INPUT|w|s|a|d|shoot|mouseWorldX|mouseWorldY|movement` (SNEAK, WALK, SPRINT, DASH; legacy messages without movement select WALK)
- client -> server: `SPAWN|class|weapon|armor` (accepted only when not alive; armor is NONE, KEVLAR or PLATED and defaults to KEVLAR when omitted)
- server -> client: `STATE|highestSpawnedPlayerId|id,x,y,hp,ammo,loot,type,maxHealth,angle,weapon,armor,armorDurability,armorMax;...|bulletId,x,y;...|lootX,lootY,lootType;...`
- server -> client: `WELCOME|playerId|mapSeed`
- server -> client: `EVENT|type|amount` (DAMAGED, DIED, LOOT_COLLECTED, HEALTH_COLLECTED, AMMO_EMPTY, POWERUP, ARMOR_BROKEN, UNDO_COMPLETED)
- server -> client: `SPAWNED` or `DIED`

For example: `SPAWN|LIGHT|SHOTGUN|PLATED`. The server sends WELCOME on connection,
but does not create a body until a valid spawn request. Angles are in radians.

This is intentionally a compact prototype rather than production netcode:
there is no prediction/reconciliation, authentication, matchmaking, encryption,
or lag compensation.

## Checks

The only demo class is `prototype.PrototypeDemo` (shallow versus deep copy, see DESIGN_PATTERNS.md); there
are no tests. To check a change, compile, start the server
(`--training` is handy: nobody dies and the bots move on their own) and connect one or more clients with
`game.Main`.

## Added design patterns

Each pattern has its own package (`singleton`, `factory`, `abstractfactory`, `strategy`, `observer`, `builder`, `prototype`, `decorator`, `command`, `adapter`, `facade`, `bridge`); everything else is in `game`. See [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md) for the requirement mapping, concrete
factory families, builders, Bridge/Decorator/Adapter/Command/Facade/Prototype
details, the [Observer sequence diagram](observer/SEQUENCE_DIAGRAM.md), and captured example output.

`Player` is the Observer subject: register a `PlayerObserver` with `addObserver`
and unsubscribe with `removeObserver`. Damage, death, loot pickup, and consuming
the final round publish `PlayerEvent` values. The server's client observer forwards
events to the affected player over WebSocket; the HUD displays them for three seconds.
Death also keeps the existing respawn screen behavior.

`WorldGenerator` generates the 60 by 40 tile map with 55 obstacles and no initial loot
(loot spawns over time). Each server uses a time-based seed and sends it in WELCOME so
clients reproduce the same walls. Example:

```java
World world = new WorldGenerator().seed(7).generate();
```

Each weapon owns a bullet Prototype. `Bullet.copy` preserves its speed and damage,
assigns the shot's unique ID, owner, position, and direction, and starts it alive.
Every pellet is an independent copy; moving or destroying one leaves other bullets
and the prototype unchanged. The nested `BulletModifiers` is copied too in the
default `DEEP` mode; `Bullet.setCopyMode(CopyMode.SHALLOW)` makes copies share it
(see DESIGN_PATTERNS.md for the address comparison). Existing weapon speeds, damage, spread, and ammo costs
are preserved.
