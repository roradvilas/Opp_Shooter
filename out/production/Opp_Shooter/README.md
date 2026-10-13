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
- server -> client: `STATE|lastAssignedPlayerId|id,x,y,hp,ammo,loot,type,maxHealth,angle,weapon;...|bulletId,x,y;...|lootX,lootY;...`
- server -> client: `WELCOME|playerId`
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
```

Checks cover player stats, normalized movement, wall collisions and sliding,
server spawning of both types over WebSocket connections, weapon counts/rates,
ammo consumption, randomized spread, unique bullet IDs, loot rendering, aim
synchronization, body removal, and respawning with a new loadout.
