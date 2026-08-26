package copper.launch.builder;

import copper.loader.func.*;
import java.util.*;

/**
 * A simple in-memory pool of class name → bytecode.
 *
 * <p>Used for the intermediate dex data that is passed between
 * {@link DexCompiler}, {@link BaseDexPool} and {@link MixinDex}.</p>
 */
public class CodePool {
    private Map<String, byte[]> codes;

    public CodePool() {
        codes = new HashMap<>();
    }

    /** Stores bytecode under a class name (slashes are converted to dots). */
    public void putCode(String className, byte[] code) {
        className = className.replace('/', '.');
        this.codes.put(className, code);
    }

    /** Whether bytecode for a class name is stored. */
    public boolean hasCode(String className) {
        className = className.replace('/', '.');
        return codes.containsKey(className);
    }

    /** Iterates over every stored class. */
    public void eachCode(ThrowableCons2<String, byte[]> cons) {
        try {
            for (var entry : codes.entrySet())
                cons.get(entry.getKey(), entry.getValue());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}