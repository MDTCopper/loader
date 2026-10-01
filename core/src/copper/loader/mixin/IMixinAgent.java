package copper.loader.mixin;

import copper.loader.func.*;
import java.lang.instrument.*;

public interface IMixinAgent {
    void setup(Instrumentation instrumentation, Func<ClassFileTransformer, ClassFileTransformer> wrapper);
    byte[] getStubClassBytecode(Class<?> clazz);
    ClassLoader getStubLoader();
}
