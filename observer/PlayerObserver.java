package observer;

@FunctionalInterface
public interface PlayerObserver {
    void onPlayerEvent(PlayerEvent event);
}
