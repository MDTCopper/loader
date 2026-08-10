package copper.loader.mixin;

import copper.loader.func.*;

public interface IMixinEngine {
    void setBytecodeProvider(Func<String, byte[]> bytecodeProvider);
    void addConfig(String config, String id);
    void bootstrap();
    byte[] transform(String name, byte[] code);
    void setLogEnabled(boolean value);
    void setEngineId(String id);
    void setFlag(String flag);
}
