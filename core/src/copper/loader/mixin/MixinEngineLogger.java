package copper.loader.mixin;

import org.spongepowered.asm.logging.*;
import copper.loader.util.*;
import java.util.*;

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
        map.put(Level.TRACE, Log.Level.DEBUG);
    }

    public String getType() {
        return "Copper Logger";
    }

    public void log(Level level, String text, Object ...args) {
        if (!MixinEngine.enableLog && (level != Level.ERROR && level != Level.FATAL))
            return;
        Log.Level lv = map.get(level);
        if (lv.ordinal() > Log.getLevel().ordinal())
            return;
        Log.log(lv, prefix + MixinEngine.id, text.replace("{}", "%s"), args);
    }
    public void log(Level level, String message, Throwable t) {
        log(level, message);
        t.printStackTrace();
    }

    public void catching(Level level, Throwable t) {
        log(level, "Catching ".concat(t.toString()), t);
    }

    public <T extends Throwable> T throwing(T t) {
        log(Level.ERROR, "Throwing ".concat(t.toString()), t);
        return t;
    }
}
