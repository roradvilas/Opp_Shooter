package command;

/** Command pattern: a reversible action on a {@link Player}; both calls return a log-friendly description. */
public interface Command {
    String execute();
    String undo();
}
