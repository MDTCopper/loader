package copper.loader.mixin;

import java.util.*;
import org.spongepowered.asm.service.*;

/**
 * Global property service backed by {@link MixinEngine#property}, a simple string-keyed map.
 */
public class MixinEngineGlobalPropertyService implements IGlobalPropertyService {
    @Override
    public IPropertyKey resolveKey(String name) {
        return new MixinStringPropertyKey(name);
    }

    private String keyString(IPropertyKey key) {
        return ((MixinStringPropertyKey)key).key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(IPropertyKey key) {
        return (T)MixinEngine.property.get(keyString(key));
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        MixinEngine.property.put(keyString(key), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T)MixinEngine.property.getOrDefault(keyString(key), defaultValue);
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object o = MixinEngine.property.get(keyString(key));
        return (o != null) ? o.toString() : defaultValue;
    }

    /** Simple string-based property key. */
    public static class MixinStringPropertyKey implements IPropertyKey {
        public final String key;

        public MixinStringPropertyKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MixinStringPropertyKey))
                return false;
            return Objects.equals(this.key, ((MixinStringPropertyKey)obj).key);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public String toString() {
            return this.key;
        }
    }
}
