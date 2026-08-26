package copper.loader.container;

import copper.loader.*;
import copper.loader.container.info.*;
import copper.loader.mixin.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A {@link Container} extended with mixin-based bytecode transformation.
 *
 * <p>In addition to the dependency traversal and resource access inherited from
 * {@link Container}, a {@code MixinContainer} holds {@link copper.loader.container.info.MixinInfo mixin configs}
 * targeting itself or other containers. During {@link #init()}, a dedicated
 * {@link IMixinEngine} is created to apply those configs. When {@link #getOwnBytecode(String)}
 * is called, raw bytecode is transparently transformed through the mixin pipeline
 * and cached.</p>
 *
 * <p>{@link #getAccessibleClass(String)} and {@link #getAccessibleBytecode(String)}
 * extend the parent search order: mixin target containers are searched first,
 * then falls back to dependency containers.</p>
 */
public abstract class MixinContainer extends Container {
    /** Mixin configurations targeting this container. */
    public List<MixinInfo> mixins;

    protected IMixinEngine mixinEngine;
    protected boolean mixinLogEnabled;
    protected List<String> mixinFlag;
    protected Map<String, byte[]> transformedBytecode;

    MixinContainer() {
        mixins = new ArrayList<>();
        mixinEngine = null;
        mixinLogEnabled = false;
        mixinFlag = new ArrayList<>();
        transformedBytecode = new ConcurrentHashMap<>();
    }

    /**
     * Bootstraps the mixin engine if any mixin configs are registered, then delegates to the parent init.
     */
    @Override
    public void init() {
        if (!mixins.isEmpty()) {
            mixinEngine = Loader.platform.createMixinEngine();
            mixinEngine.setBytecodeProvider(name -> {
                if (name.startsWith("/"))
                    name = name.substring(1);
                name = name.replace('/', '.');

                // Get raw bytecode
                byte[] code = super.getOwnBytecode(name);
                if (code == null)
                    code = getAccessibleBytecode(name);
                return code;
            });
            mixinEngine.setEngineId(id);
            mixinEngine.setLogEnabled(mixinLogEnabled);
            mixinEngine.bootstrap();
            for (String flag : mixinFlag)
                mixinEngine.setFlag(flag);
            for (MixinInfo info : mixins)
                mixinEngine.addConfig(info.config, info.container.id.replace(':', '-'));
        }
        super.init();
    }

    /**
     * Searches mixin target containers first, then falls back to dependency containers.
     */
    @Override
    public Class<?> getAccessibleClass(String name) {
        Class<?> c = null;
        // Search mixin target containers first.
        for (MixinInfo info : mixins) {
            c = info.container.loadPublicOwnClass(name);
            if (c != null)
                break;
        }
        // Fall back to dependency containers.
        if (c == null)
            c = super.getAccessibleClass(name);
        return c;
    }

    /**
     * Searches mixin target containers first, then falls back to dependency containers.
     */
    @Override
    public byte[] getAccessibleBytecode(String name) {
        byte[] code = null;
        // Search mixin target containers first.
        for (MixinInfo info : mixins) {
            code = info.container.getPublicOwnBytecode(name);
            if (code != null)
                break;
        }
        // Fall back to dependency containers.
        if (code == null)
            code = super.getAccessibleBytecode(name);
        return code;
    }

    /**
     * Returns mixin-transformed bytecode. Raw bytecode is fetched from the parent,
     * transformed through the mixin pipeline, and cached for subsequent calls.
     */
    @Override
    public byte[] getOwnBytecode(String name) {
        byte[] code = transformedBytecode.get(name);
        if (code == null) {
            code = super.getOwnBytecode(name);
            // if container is ready, try transform
            // or just return raw byte code
            if (mixinEngine != null) {
                byte[] transformed = mixinEngine.transform(name, code);
                if (transformed != null) {
                    transformedBytecode.put(name, transformed);
                    code = transformed;
                }
            }
        }
        return code;
    }

    /** Enables or disables mixin audit logging for this container. */
    public void setMixinLogEnabled(boolean v) {
        if (mixinEngine != null)
            mixinEngine.setLogEnabled(v);
        mixinLogEnabled = v;
    }

    /** Adds a mixin environment flag (e.g. {@code "EXPORT_FILTER"}). */
    public void addMixinFlag(String v) {
        if (mixinEngine != null)
            mixinEngine.setFlag(v);
        mixinFlag.add(v);
    }
}
