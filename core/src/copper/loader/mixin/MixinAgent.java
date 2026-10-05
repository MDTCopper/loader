package copper.loader.mixin;

import copper.loader.func.*;
import java.lang.instrument.*;

public class MixinAgent implements IMixinAgent {
    private static boolean setup = false;
    private static final MixinAgent instance = new MixinAgent();

    /**
     * Registers the framework's hot swap agent on the JVM instrumentation, once per engine.
     *
     * <p>Must run after the engine bootstrapped: the framework creates its agent instance while it
     * builds the mixin transformer, so an earlier call would leave that instance to register its
     * transformer directly, bypassing the wrapper. A second call would register the wrapper twice and
     * re-apply mixins to every class the JVM redefines.</p>
     */
    public void setup(Instrumentation instrumentation, Func<ClassFileTransformer, ClassFileTransformer> wrapper) {
        if (setup || !MixinEngine.isStarted())
            return;
        var wrapped = new MixinInstrumentation(instrumentation, wrapper);
        org.spongepowered.tools.agent.MixinAgent.init(wrapped);
        setup = true;
    }

    @Override
    public byte[] getStubClassBytecode(Class<?> clazz) {
        return org.spongepowered.tools.agent.MixinAgent.getStubClassBytecode(clazz);
    }

    @Override
    public ClassLoader getStubLoader() {
        return org.spongepowered.tools.agent.MixinAgent.getClassLoader();
    }

    public static MixinAgent getInstance() {
        return instance;
    }
}
