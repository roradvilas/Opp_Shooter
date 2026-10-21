package observer;

/** Observer pattern subject: publishes {@link PlayerEvent}s to subscribed {@link PlayerObserver}s. */
public interface PlayerSubject {
    void addObserver(PlayerObserver observer);
    void removeObserver(PlayerObserver observer);
    void notifyObservers(PlayerEvent event);
}
