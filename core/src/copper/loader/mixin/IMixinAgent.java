package copper.loader.mixin;

import copper.loader.func.*;
import java.lang.instrument.*;

/**
 * The framework's hot swap agent as seen from the loader: its implementation lives in a container's
 * isolated classloader, so only this interface is shared with the loader.
 */
public interface IMixinAgent {
    /**
     * Hands the instrumentation to the agent, once per engine.
     *
     * @param wrapper wraps every transformer the agent registers
     */
    void setup(Instrumentation instrumentation, Func<ClassFileTransformer, ClassFileTransformer> wrapper);

    /** Bytecode of the stub the framework defines for a mixin class; {@code null} when the class is not a mixin. */
    byte[] getStubClassBytecode(Class<?> clazz);

    /** The loader the framework defines its stub mixin classes in. */
    ClassLoader getStubLoader();
}
