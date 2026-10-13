# Hell Is Others Lite — raw WebSocket PvP

This version keeps the original Java/Swing client, but turns the game into a
server-authoritative PvP multiplayer game.

## Run

Requires JDK 17+.

Compile:
```bash
javac --release 17 *.java
```

Start the server:
```bash
java ServerMain
```

Then, in two or more terminals:
```bash
java Main
```

By default clients connect to `localhost:8080`.

The client opens an in-game spawn screen with class and weapon selectors. Heavy keeps the
original stats (100 HP, speed 3.2 per tick); Light has 80 HP and speed 3.84 per
tick. Both have the same ammo, shooting delay, and collision radius. The server
creates the selected type through the abstract `PlayerFactory` and its concrete
`HeavyPlayerFactory` and `LightPlayerFactory` implementations.

Choose a loadout and click Spawn to enter the world. On death, your body disappears
and the same screen returns with a Respawn button. You can change class and weapon
before each respawn; health, ammo, and collected loot reset. Aiming follows the
mouse and is synchronized to other clients. Picked-up loot disappears for everyone.

The map starts with no loot. The server spawns one pickup every five seconds on
an open floor tile more than two tiles (80 pixels) from every existing pickup,
randomly choosing ammo or health with equal probability. This circular exclusion
radius applies to both loot types and includes positions exactly two tiles away.
Gold boxes give 5 ammo; green boxes with a white cross restore up to 25 HP,
capped at the selected class's maximum health. Full-health players leave health
pickups on the ground. Both types increase the loot counter when collected and
show a pickup notification. Loot remains until collected; spawning is skipped when
no floor tile satisfies the spacing rule.

Weapons implement `WeaponStrategy` and share the server's bullet ID counter and
the player's ammo pool. Shooting speed means fire rate; all bullets retain the
original velocity and damage:

| Weapon | Bullets per shot | Fire rate | Delay | Ammo per shot |
| --- | --- | --- | --- | --- |
| Assault Rifle | 1 | 100% | 130 ms | 1 |
| Pistol | 1 | 40% | 325 ms | 1 |
| Shotgun | 3 | 20% | 650 ms | 3 |

The shotgun fires a center pellet and two side pellets, each offset randomly by
3–15 degrees from the aim direction. It requires three rounds to fire a full shot.

For another machine, change `SERVER_HOST` in `Game.java` to the server's IP.

## Networking

There is deliberately **no WebSocket framework/library**. `WebSocketServer`
implements the RFC 6455 opening handshake and basic text-frame parsing/writing
directly on `ServerSocket`/`Socket`.

The server owns authoritative player positions, shooting, bullets, HP, ammo and
loot. Clients send input snapshots; the server simulates the world and broadcasts
snapshots.

Console messages use the singleton `Logger` via `Logger.getInstance().log(text)`.

Protocol:
- client -> server: `INPUT|w|s|a|d|shoot|mouseWorldX|mouseWorldY`
- client -> server: `SPAWN|class|weapon` (accepted only when not alive)
- server -> client: `STATE|lastAssignedPlayerId|id,x,y,hp,ammo,loot,type,maxHealth,angle,weapon;...|bulletId,x,y;...|lootX,lootY,lootType;...`
- server -> client: `WELCOME|playerId|mapSeed`
- server -> client: `EVENT|type|amount` (DAMAGED, DIED, LOOT_COLLECTED, HEALTH_COLLECTED, AMMO_EMPTY)
- server -> client: `SPAWNED` or `DIED`

For example: `SPAWN|LIGHT|SHOTGUN`. The server sends WELCOME on connection,
but does not create a body until a valid spawn request. Angles are in radians.

This is intentionally a compact prototype rather than production netcode:
there is no prediction/reconciliation, authentication, matchmaking, encryption,
or lag compensation.

## Checks

```bash
javac --release 17 -d out/tests *.java tests/*.java
java -cp out/tests PlayerFactoryTest
java -cp out/tests GameplayTest
java -cp out/tests DesignPatternsTest
java -cp out/tests LootTest
```

Checks cover player stats, normalized movement, wall collisions and sliding,
server spawning of both types over WebSocket connections, weapon counts/rates,
ammo consumption, randomized spread, unique bullet IDs, loot rendering, aim
synchronization, body removal, and respawning with a new loadout.
Loot checks also cover five-second timer boundaries, spawn locations, both pickup
types, healing caps, full-health pickup retention, and typed loot rendering.

## Added design patterns

`Player` is the Observer subject: register a `PlayerObserver` with `addObserver`
and unsubscribe with `removeObserver`. Damage, death, loot pickup, and consuming
the final round publish `PlayerEvent` values. The server's client observer forwards
events to the affected player over WebSocket; the HUD displays them for three seconds.
Death also keeps the existing respawn screen behavior.

`WorldBuilder` generates independent maps with configurable tile dimensions,
seed, obstacle count, and loot placement attempts. Defaults keep the 60 by 40
map, 55 obstacles, and no initial loot. Each server uses a time-based seed and
sends it in WELCOME so clients reproduce the same walls. Example:

```java
World world = new WorldBuilder().size(60, 40).seed(7)
        .obstacles(55).build();
```

Each weapon owns a bullet Prototype. `Bullet.copy` preserves its speed and damage,
assigns the shot's unique ID, owner, position, and direction, and starts it alive.
Every pellet is an independent copy; moving or destroying one leaves other bullets
and the prototype unchanged. Existing weapon speeds, damage, spread, and ammo costs
are preserved.
