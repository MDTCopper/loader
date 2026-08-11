package copper.loader.container;

import copper.loader.*;
import copper.loader.container.info.*;
import copper.loader.mixin.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A pluggable classpath container providing class loading, resource access,
 * and dependency traversal between containers.
 *
 * <p>Each container owns a set of {@link copper.loader.container.info.DependencyInfo dependencies}
 * (other containers it can search for classes), a set of
 * {@link copper.loader.container.info.MixinInfo mixin configs} (bytecode transformations
 * to apply), a {@link ResourceProvider}, and a {@link ClassFilter} controlling export
 * visibility.</p>
 */
public abstract class MixinContainer extends Container {
    /** Mixin configurations targeting this container. */
    public List<MixinInfo> mixin;

    protected IMixinEngine mixinEngine;
    protected boolean mixinLogEnabled;
    protected List<String> mixinFlag;
    protected Map<String, byte[]> transformedBytecode;

    MixinContainer() {
        mixin = new ArrayList<>();
        mixinEngine = null;
        mixinLogEnabled = false;
        mixinFlag = new ArrayList<>();
        transformedBytecode = new ConcurrentHashMap<>();
    }

    @Override
    public void init() {
        if (!mixin.isEmpty()) {
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
            for (MixinInfo info : mixin)
                mixinEngine.addConfig(info.config, info.container.id.replace(':', '-'));
        }
        super.init();
    }

    @Override
    public Class<?> getAccessibleClass(String name) {
        Class<?> c = null;
        // Search mixin target containers first.
        for (MixinInfo info : mixin) {
            c = info.container.loadPublicOwnClass(name);
            if (c != null)
                break;
        }
        // Fall back to dependency containers.
        if (c == null)
            c = super.getAccessibleClass(name);
        return c;
    }

    @Override
    public byte[] getAccessibleBytecode(String name) {
        byte[] code = null;
        // Search mixin target containers first.
        for (MixinInfo info : mixin) {
            code = info.container.getPublicOwnBytecode(name);
            if (code != null)
                break;
        }
        // Fall back to dependency containers.
        if (code == null)
            code = super.getAccessibleBytecode(name);
        return code;
    }

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
