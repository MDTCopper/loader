package copper.loader.util;

import copper.loader.func.*;

public class Log {

    public enum Level {
        ERROR, WARN, INFO, DEBUG
    }

    private static Level level = Level.INFO;
    private static Cons2<Level, String> backend = Backend::plain;

    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";

    public static void setLevel(Level level) {
        Log.level = level;
    }

    public static Level getLevel() {
        return level;
    }

    public static void setBackend(Cons2<Level, String> backend) {
        Log.backend = backend;
    }

    public static void log(Level level, String msg) {
        if (level.ordinal() <= Log.level.ordinal()) {
            backend.get(level, " " + msg);
        }
    }

    public static void log(Level level, String tag, String msg) {
        if (tag != null && !tag.isEmpty()) {
            log(level, "[" + tag + "] " + msg);
        } else {
            log(level, msg);
        }
    }

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
            case INFO:  return BLUE;
            case WARN:  return YELLOW;
            case ERROR: return RED;
            case DEBUG: return CYAN;
            default:    return RESET;
        }
    }

    static String prefixForLevel(Level level) {
        switch (level) {
            case INFO:  return "[I]";
            case WARN:  return "[W]";
            case ERROR: return "[E]";
            case DEBUG: return "[D]";
            default:    return "";
        }
    }

    public static class Backend {
        public static void plain(Level level, String txt) {
            String colored = Log.colorForLevel(level) + Log.prefixForLevel(level) + Log.RESET + txt;
            System.out.println(colored);
        }

        public static void colorless(Level level, String txt) {
            System.out.println(Log.prefixForLevel(level) + txt);
        }
    }
}
