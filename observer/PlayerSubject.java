package observer;

/** Observer pattern subject: publishes {@link PlayerEvent}s to subscribed {@link PlayerObserver}s. */
public interface PlayerSubject {
    void addObserver(PlayerObserver observer);
    void removeObserver(PlayerObserver observer);
    /** Unsubscribes every observer before this subject is removed. */
    void removeAllObservers();
    void notifyObservers(PlayerEvent event);
}
