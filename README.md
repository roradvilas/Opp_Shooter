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

Choose Heavy or Light in the selection dialog before connecting. Heavy keeps the
original stats (100 HP, speed 3.2 per tick); Light has 80 HP and speed 3.84 per
tick. Both have the same ammo, shooting delay, and collision radius. The server
creates the selected type through the abstract `PlayerFactory` and its concrete
`HeavyPlayerFactory` and `LightPlayerFactory` implementations.

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
- client -> server: `JOIN|HEAVY` or `JOIN|LIGHT` (once, before spawning)
- server -> client: `STATE|lastAssignedPlayerId|id,x,y,hp,ammo,loot,type,maxHealth;...|bulletId,x,y;...|lootX,lootY;...`
- server -> client: `WELCOME|playerId`

This is intentionally a compact prototype rather than production netcode:
there is no prediction/reconciliation, authentication, matchmaking, encryption,
or lag compensation.

## Checks

```bash
javac --release 17 -d out/tests *.java tests/PlayerFactoryTest.java
java -cp out/tests PlayerFactoryTest
```

Checks cover player stats, normalized movement, wall collisions and sliding,
and server spawning of both types over WebSocket connections.
