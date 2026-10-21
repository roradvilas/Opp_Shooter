package command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Invoker: runs commands and remembers the most recent {@value #CAPACITY} so they can be undone. */
public final class CommandInvoker {
    public static final int CAPACITY = 100;

    private final Deque<Command> history = new ArrayDeque<>();

    public synchronized String run(Command command) {
        String result = Objects.requireNonNull(command).execute();
        history.addLast(command);
        if (history.size() > CAPACITY) history.removeFirst();
        return result;
    }

    /** Undoes the most recent command, or returns null if there is nothing to undo. */
    public synchronized String undo() {
        Command command = history.pollLast();
        return command == null ? null : command.undo();
    }

    public synchronized int historySize() { return history.size(); }
}
