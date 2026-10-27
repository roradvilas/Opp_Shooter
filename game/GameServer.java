package game;

import abstractfactory.CombatFactory;
import bridge.Player;
import facade.GameFacade;
import facade.PlayerInput;
import facade.Protocol;
import observer.PlayerEvent;
import observer.PlayerObserver;
import singleton.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Sockets and threads; message formats live in {@link Protocol} and all game rules behind the {@link GameFacade}. */
public class GameServer {
    private final int port;
    private final GameFacade game;
    private final BotController bots;
    private final Map<Integer, Client> clients = new ConcurrentHashMap<>();
    private int nextPlayerId = 1;

    public GameServer(int port) {
        this(port, new GameFacade(new CombatFactory()));
    }

    public GameServer(int port, GameFacade game) {
        this(port, game, null);
    }

    /** {@code bots} may be null for a server without bots. */
    public GameServer(int port, GameFacade game, BotController bots) {
        this.port = port;
        this.game = Objects.requireNonNull(game);
        this.bots = bots;
    }

    public void start() throws IOException {
        Logger.getInstance().log("PvP server listening on ws://0.0.0.0:" + port);
        try (ServerSocket server = new ServerSocket(port)) {
            new Thread(this::gameLoop, "game-loop").start();
            while (true) {
                Socket socket = server.accept();
                try {
                    WebSocketServer ws = new WebSocketServer(socket);
                    if (!ws.handshake()) { socket.close(); continue; }

                    int id = nextPlayerId++;
                    Client client = new Client(id, ws);
                    clients.put(id, client);
                    client.send(Protocol.welcome(id, game.worldSeed()));
                    new Thread(client, "player-" + id).start();

                    Logger.getInstance().log("Player " + id + " connected.");
                } catch (Exception e) {
                    try { socket.close(); } catch (IOException ignored) {}
                    e.printStackTrace();
                }
            }
        }
    }

    private void gameLoop() {
        final long tick = 1000 / 60;
        long next = System.currentTimeMillis();

        while (true) {
            long now = System.currentTimeMillis();
            if (now >= next) {
                update(now);
                broadcastState();
                next += tick;
                if (now - next > 1000) next = now;
            } else {
                try { Thread.sleep(Math.min(2, next - now)); }
                catch (InterruptedException ignored) {}
            }
        }
    }

    private synchronized void update(long now) {
        if (bots != null) bots.update(now);
        for (Client c : clients.values()) game.applyInput(c.id, c.input, now);

        for (int id : game.tick(now)) {
            Client victim = clients.get(id);
            if (victim != null) {
                victim.input = PlayerInput.idle(victim.input.mouseX(), victim.input.mouseY());
                try { victim.send(Protocol.DIED); }
                catch (IOException e) { disconnect(id); }
            }
        }
    }

    private synchronized void broadcastState() {
        String state = game.stateMessage();
        for (Client c : clients.values()) {
            try { c.send(state); }
            catch (Exception e) { disconnect(c.id); }
        }
    }

    private synchronized void disconnect(int id) {
        Client c = clients.remove(id);
        game.remove(id);
        if (c != null) try { c.ws.close(); } catch (Exception ignored) {}
        Logger.getInstance().log("Player " + id + " disconnected.");
    }

    private synchronized void handleMessage(Client client, String msg) throws IOException {
        if (!clients.containsKey(client.id)) return;
        Protocol.SpawnRequest request = Protocol.parseSpawn(msg);
        if (request != null) {
            if (game.hasPlayer(client.id)) return;
            Player player = game.spawn(client.id, request.type(), request.weapon(), request.armor());
            player.addObserver(client);
            client.input = PlayerInput.idle(player.x + 100, player.y);
            client.send(Protocol.SPAWNED);
        } else if (game.hasPlayer(client.id)) {
            PlayerInput input = Protocol.parseInput(msg);
            if (input != null) client.input = input;
        }
    }

    private class Client implements Runnable, PlayerObserver {
        final int id;
        final WebSocketServer ws;
        volatile PlayerInput input = PlayerInput.idle(0, 0);

        Client(int id, WebSocketServer ws) {
            this.id = id; this.ws = ws;
        }

        void send(String text) throws IOException { ws.sendText(text); }

        @Override public void onPlayerEvent(PlayerEvent event) {
            try { send(Protocol.event(event)); }
            catch (IOException e) { disconnect(id); }
        }

        @Override public void run() {
            try {
                while (true) {
                    String msg = ws.readText();
                    if (msg == null) break;
                    handleMessage(this, msg);
                }
            } catch (Exception ignored) {
            } finally {
                disconnect(id);
            }
        }
    }
}
