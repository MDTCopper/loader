package copper.loader.mixin;

import org.spongepowered.asm.logging.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;

/**
 * Bridges Mixin's logging to Copper's {@link Log} system.
 *
 * <p>Non-error log messages are suppressed unless {@link MixinEngine#enableLog} is {@code true}.
 * Slf4j-style {@code {}} placeholders are converted to printf-style {@code %s}.</p>
 */
public class MixinEngineLogger extends LoggerAdapterAbstract {
    private Map<Level, Log.Level> map;
    private String prefix;

    public MixinEngineLogger(String name) {
        super(name);
        prefix = name + ":";

        map = new HashMap<>();
        map.put(Level.INFO, Log.Level.INFO);
        map.put(Level.WARN, Log.Level.WARN);
        map.put(Level.ERROR, Log.Level.ERROR);
        map.put(Level.FATAL, Log.Level.ERROR);
        map.put(Level.DEBUG, Log.Level.DEBUG);
        map.put(Level.TRACE, Log.Level.VERBOSE);
    }

    @Override
    public String getType() {
        return "Copper Logger";
    }

    @Override
    public void log(Level level, String text, Object ...args) {
        if (!MixinEngine.enableLog && (level != Level.ERROR && level != Level.FATAL))
            return;
        Log.Level lv = map.get(level);
        if (lv.ordinal() > Log.getLevel().ordinal())
            return;
        Log.log(lv, prefix + MixinEngine.id, text.replace("{}", "%s"), args);
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        if (map.get(level).ordinal() > Log.getLevel().ordinal())
            return;
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        log(level, message + "\n" + writer);
    }

    @Override
    public void catching(Level level, Throwable t) {
        log(level, "Catching ".concat(t.toString()), t);
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        log(Level.ERROR, "Throwing ".concat(t.toString()), t);
        return t;
    }
}
