package copper.loader.mixin;

import copper.loader.func.*;

public class MockMixinEngine implements IMixinEngine {
    @Override
    public void setBytecodeProvider(Func<String, byte[]> bytecodeProvider) {}

    @Override
    public void addConfig(String config, String id) {}

    @Override
    public void bootstrap() {}

    @Override
    public byte[] transform(String name, byte[] code) {
        return new byte[0];
    }

    @Override
    public void setLogEnabled(boolean value) {}

    @Override
    public void setEngineId(String id) {}

    @Override
    public void setFlag(String flag) {}
}
