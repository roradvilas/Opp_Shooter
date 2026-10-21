package singleton;

/**
 * Singleton. Thread-safe because the instance is created by static initialization, which the JVM
 * runs exactly once under the class-initialization lock before any thread can call
 * {@link #getInstance()}; {@link #log} is synchronized so concurrent lines never interleave.
 */
public final class Logger {
    private static final Logger INSTANCE = new Logger();

    private Logger() {}

    public static Logger getInstance() {
        return INSTANCE;
    }

    public synchronized void log(String text) {
        System.out.println(text);
    }
}
