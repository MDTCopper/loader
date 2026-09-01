package copper.loader.mixin;

import copper.loader.func.*;

/**
 * A no-op {@link IMixinEngine} used at runtime on Android.
 *
 * <p>All mixins were already applied to the dex during the build phase, so the
 * runtime never transforms anything: {@link #transform} just returns an empty
 * array and every other call does nothing.</p>
 */
public class MockMixinEngine implements IMixinEngine {
    @Override
    public void setBytecodeProvider(Func<String, byte[]> bytecodeProvider) {}

    @Override
    public void addConfig(String config, String id) {}

    @Override
    public void bootstrap() {}

    @Override
    public byte[] transform(String name, byte[] code) {
        return null;
    }

    @Override
    public void setLogEnabled(boolean value) {}

    @Override
    public void setEngineId(String id) {}

    @Override
    public void setFlag(String flag) {}
}