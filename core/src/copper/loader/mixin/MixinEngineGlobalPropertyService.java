package copper.loader.mixin;

import java.util.*;
import org.spongepowered.asm.service.*;

public class MixinEngineGlobalPropertyService implements IGlobalPropertyService {
    public IPropertyKey resolveKey(String name) {
        return new MixinStringPropertyKey(name);
    }

    private String keyString(IPropertyKey key) {
        return ((MixinStringPropertyKey)key).key;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return (T)MixinEngine.property.get(keyString(key));
    }

    public void setProperty(IPropertyKey key, Object value) {
        MixinEngine.property.put(keyString(key), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T)MixinEngine.property.getOrDefault(keyString(key), defaultValue);
    }

    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object o = MixinEngine.property.get(keyString(key));
        return (o != null) ? o.toString() : defaultValue;
    }

    public static class MixinStringPropertyKey implements IPropertyKey {
        public final String key;

        public MixinStringPropertyKey(String key) {
            this.key = key;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof MixinStringPropertyKey))
                return false;
            return Objects.equals(this.key, ((MixinStringPropertyKey)obj).key);
        }

        public int hashCode() {
            return this.key.hashCode();
        }

        public String toString() {
            return this.key;
        }
    }
}

