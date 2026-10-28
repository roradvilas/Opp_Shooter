# Observer sequence diagram

`Player` is the subject and the spawned connection's `GameServer.Client` is its observer. The
diagram follows a bullet hit from registration through the HUD. The same notification route is used
for loot, health, power-up, empty-ammo, armor-break, and invalid-move events.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Game UI
    participant ClientFacade
    participant ClientWS as WebSocketClient
    participant ServerWS as WebSocketServer
    participant Observer as GameServer.Client (PlayerObserver)
    participant Server as GameServer
    participant Facade as GameFacade
    participant Protocol
    participant Player as Player (PlayerSubject)

    UI->>ClientFacade: spawn(type, weapon, armor)
    ClientFacade->>ClientWS: sendText(SPAWN request)
    ClientWS->>ServerWS: SPAWN request
    Observer->>ServerWS: readText()
    ServerWS-->>Observer: SPAWN request
    Observer->>Server: handleMessage(client, request)
    Server->>Facade: spawn(id, type, weapon, armor)
    Facade-->>Server: Player
    Server->>Player: addObserver(client)
    Server->>Observer: send(SPAWNED)
    Observer->>ServerWS: sendText(SPAWNED)
    ServerWS-->>ClientWS: SPAWNED
    ClientWS->>ClientFacade: onText(SPAWNED)
    ClientFacade->>UI: onSpawned()

    Server->>Facade: tick(now)
    Facade->>Player: damage(bullet.effectiveDamage())
    Player->>Player: armor.absorb(amount), update health
    Player->>Player: create PlayerEvent(DAMAGED, actual damage)
    Player->>Player: notifyObservers(event)
    loop each registered observer
        Player->>Observer: onPlayerEvent(event)
        Observer->>Protocol: event(event)
        Protocol-->>Observer: EVENT|DAMAGED|amount
        Observer->>ServerWS: sendText(EVENT|DAMAGED|amount)
    end
    ServerWS-->>ClientWS: EVENT|DAMAGED|amount
    ClientWS->>ClientFacade: onText(EVENT|DAMAGED|amount)
    ClientFacade->>ClientFacade: create PlayerEvent using known playerId
    ClientFacade->>UI: onEvent(event)
    UI->>UI: show event.message() for 3 seconds

    opt player's health reaches zero
        Player->>Player: create PlayerEvent(DIED, 0)
        Player->>Player: notifyObservers(DIED event)
        loop each registered observer
            Player->>Observer: onPlayerEvent(DIED, 0)
            Observer->>Protocol: event(DIED)
            Protocol-->>Observer: EVENT|DIED|0
            Observer->>ServerWS: sendText(EVENT|DIED|0)
        end
        ServerWS-->>ClientWS: EVENT|DIED|0
        ClientWS->>ClientFacade: onText(EVENT|DIED|0)
        ClientFacade->>UI: onEvent(DIED)
        Facade->>Facade: remove player and collect killed id
        Facade-->>Server: return killed ids
        Server->>Observer: send(DIED)
        Observer->>ServerWS: sendText(DIED)
        ServerWS-->>ClientWS: DIED
        ClientWS->>ClientFacade: onText(DIED)
        ClientFacade->>UI: onDied()
        UI->>UI: show respawn screen
    end
```

`Player.addObserver` runs after `GameFacade.spawn` returns the new player. `notifyObservers` iterates
the player's `CopyOnWriteArrayList` and invokes callbacks synchronously on the publishing thread.
`Player.damage` publishes `DAMAGED`, then `ARMOR_BROKEN` if armor just broke, then `DIED` if health
reached zero. `Player.collectLoot` and `Player.shoot` publish their corresponding events;
`GameFacade.rejectMove` publishes `UNDO_COMPLETED` directly. The wire event contains type and amount;
`ClientFacade` supplies the player id it learned from `WELCOME` when it reconstructs the `PlayerEvent`.

The `DIED` event is a HUD notification. After the facade reports killed ids, `GameServer` also sends
the separate `DIED` protocol message that opens the respawn screen.
