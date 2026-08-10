package copper.loader.mixin;

import copper.loader.func.*;

/**
 * The mixin engine interface.
 *
 * <p>Provides bytecode transformation via the SpongePowered Mixin framework.
 * Each {@link copper.loader.container.JvmContainer} has its own engine instance.</p>
 */
public interface IMixinEngine {
    /** Sets the bytecode provider used to resolve class bytes during transformation. */
    void setBytecodeProvider(Func<String, byte[]> bytecodeProvider);

    /** Registers a mixin configuration JSON. */
    void addConfig(String config, String id);

    /** Bootstraps the mixin environment. Must be called before {@link #transform}. */
    void bootstrap();

    /**
     * Transforms a class bytecode through all registered mixins.
     *
     * @param name the fully qualified class name
     * @param code the original bytecode
     * @return the transformed bytecode, or {@code null} if no transformation was applied
     */
    byte[] transform(String name, byte[] code);

    /** Enables or disables mixin audit logging. */
    void setLogEnabled(boolean value);

    /** Sets the engine id (used in log output and debug file paths). */
    void setEngineId(String id);

    /**
     * Sets a Mixin environment option flag (e.g. {@code "EXPORT_FILTER"}).
     * All available flags can be find in {@link org.spongepowered.asm.mixin.MixinEnvironment.Option}.
     */
    void setFlag(String flag);
}
