package copper.loader.util;

import copper.loader.func.*;

/**
 * Logging utility with colorized output and configurable log level.
 *
 * <p>Supports four log levels: {@link Level#ERROR}, {@link Level#WARN}, {@link Level#INFO}, {@link Level#DEBUG}.
 * The backend can be swapped (e.g., to colorless mode for non-interactive terminals).</p>
 */
public class Log {

    public enum Level {
        ERROR, WARN, INFO, DEBUG, VERBOSE
    }

    private static Level level = Level.INFO;
    private static Cons2<Level, String> backend = Backend::plain;

    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";

    /** Sets the minimum log level. Messages below this level are suppressed. */
    public static void setLevel(Level level) {
        Log.level = level;
    }

    public static Level getLevel() {
        return level;
    }

    /** Sets the log output backend (e.g. {@link Backend#colorless}). */
    public static void setBackend(Cons2<Level, String> backend) {
        Log.backend = backend;
    }

    /** Logs a message at the given level. */
    public static void log(Level level, String msg) {
        if (level.ordinal() <= Log.level.ordinal()) {
            backend.get(level, " " + msg);
        }
    }

    /** Logs a message with a tag. */
    public static void log(Level level, String tag, String msg) {
        if (tag != null && !tag.isEmpty()) {
            log(level, "[" + tag + "] " + msg);
        } else {
            log(level, msg);
        }
    }

    /** Logs a formatted message with a tag. */
    public static void log(Level level, String tag, String format, Object... args) {
        String formatted = String.format(format, args);
        log(level, tag, formatted);
    }

    public static void error(String msg) {
        log(Level.ERROR, msg);
    }

    public static void error(String tag, String msg) {
        log(Level.ERROR, tag, msg);
    }

    public static void error(String tag, String format, Object... args) {
        log(Level.ERROR, tag, format, args);
    }

    public static void warn(String msg) {
        log(Level.WARN, msg);
    }

    public static void warn(String tag, String msg) {
        log(Level.WARN, tag, msg);
    }

    public static void warn(String tag, String format, Object... args) {
        log(Level.WARN, tag, format, args);
    }

    public static void info(String msg) {
        log(Level.INFO, msg);
    }

    public static void info(String tag, String msg) {
        log(Level.INFO, tag, msg);
    }

    public static void info(String tag, String format, Object... args) {
        log(Level.INFO, tag, format, args);
    }

    public static void debug(String msg) {
        log(Level.DEBUG, msg);
    }

    public static void debug(String tag, String msg) {
        log(Level.DEBUG, tag, msg);
    }

    public static void debug(String tag, String format, Object... args) {
        log(Level.DEBUG, tag, format, args);
    }

    static String colorForLevel(Level level) {
        switch (level) {
            case INFO:    return BLUE;
            case WARN:    return YELLOW;
            case ERROR:   return RED;
            case DEBUG:   return CYAN;
            case VERBOSE: return CYAN;
            default:      return RESET;
        }
    }

    static String prefixForLevel(Level level) {
        switch (level) {
            case INFO:    return "[I]";
            case WARN:    return "[W]";
            case ERROR:   return "[E]";
            case DEBUG:   return "[D]";
            case VERBOSE: return "[V]";
            default:      return "";
        }
    }

    /** Built-in log backends. */
    public static class Backend {
        /** Colorized output to stdout. */
        public static void plain(Level level, String txt) {
            String colored = Log.colorForLevel(level) + Log.prefixForLevel(level) + Log.RESET + txt;
            System.out.println(colored);
        }

        /** Plain-text output to stdout (no ANSI color codes). */
        public static void colorless(Level level, String txt) {
            System.out.println(Log.prefixForLevel(level) + txt);
        }
    }
}
