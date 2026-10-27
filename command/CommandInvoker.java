package command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

    /**
     * Undoes up to {@code count} of the most recent commands, newest first, and returns what each undo
     * reported. Fewer are undone when the history is shorter.
     */
    public synchronized List<String> undo(int count) {
        if (count < 0) throw new IllegalArgumentException("Undo count cannot be negative");
        List<String> undone = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String result = undo();
            if (result == null) break;
            undone.add(result);
        }
        return undone;
    }
}
