package copper.launch.builder;

import copper.loader.func.*;
import java.util.*;

public class CodePool {
    private Map<String, byte[]> code;

    public CodePool() {
        code = new HashMap<>();
    }

    public void putCode(String className, byte[] code) {
        className = className.replace('/', '.');
        this.code.put(className, code);
    }

    public boolean hasCode(String className) {
        className = className.replace('/', '.');
        return code.containsKey(className);
    }

    public void eachCode(ThrowableCons2<String, byte[]> cons) {
        try {
            for (var entry : code.entrySet())
                cons.get(entry.getKey(), entry.getValue());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
