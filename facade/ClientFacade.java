package facade;

import bridge.PlayerType;
import game.WebSocketClient;
import observer.PlayerEvent;
import strategy.WeaponType;

import java.io.IOException;
import java.util.Objects;

/**
 * Facade over the client side of the connection. The UI calls {@link #connect}, {@link #spawn} and
 * {@link #sendInput} and receives typed callbacks; the WebSocket transport, the handshake and the
 * {@link Protocol} encoding stay hidden behind it.
 */
public final class ClientFacade {
    /** What the server can tell a client. Every callback is optional. */
    public interface Listener {
        default void onWelcome(int playerId, long worldSeed) {}
        default void onSpawned() {}
        default void onDied() {}
        default void onEvent(PlayerEvent event) {}
        default void onState(GameState state) {}
        default void onDisconnected() {}
    }

    private final Listener listener;
    private final WebSocketClient socket;
    private volatile int playerId = -1;

    public ClientFacade(String host, int port, Listener listener) {
        this.listener = Objects.requireNonNull(listener);
        this.socket = new WebSocketClient(host, port, new WebSocketClient.Listener() {
            @Override public void onText(String text) { handle(text); }
            @Override public void onClose() { ClientFacade.this.listener.onDisconnected(); }
        });
    }

    public void connect() throws Exception { socket.connect(); }

    public void close() { socket.close(); }

    /** The id the server assigned, or -1 before the WELCOME message arrives. */
    public int playerId() { return playerId; }

    public void spawn(PlayerType type, WeaponType weapon) throws IOException {
        socket.sendText(Protocol.spawn(type, weapon));
    }

    public void sendInput(PlayerInput input) throws IOException {
        socket.sendText(Protocol.input(input));
    }

    private void handle(String text) {
        String[] parts = text.split("\\|", -1);
        switch (parts[0]) {
            case "WELCOME" -> {
                if (parts.length < 3) return;
                try {
                    playerId = Integer.parseInt(parts[1]);
                    listener.onWelcome(playerId, Long.parseLong(parts[2]));
                } catch (NumberFormatException ignored) {}
            }
            case "SPAWNED" -> listener.onSpawned();
            case "DIED" -> listener.onDied();
            case "EVENT" -> {
                if (parts.length != 3) return;
                try {
                    listener.onEvent(new PlayerEvent(playerId, PlayerEvent.Type.valueOf(parts[1]),
                            Integer.parseInt(parts[2])));
                } catch (IllegalArgumentException ignored) {}
            }
            case "STATE" -> {
                GameState state = Protocol.parseState(text);
                if (state != null) listener.onState(state);
            }
            default -> {}
        }
    }
}
