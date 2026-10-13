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
