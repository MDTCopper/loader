package copper.loader.mixin;

import copper.loader.func.*;
import copper.loader.util.*;
import org.spongepowered.asm.launch.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.util.*;
import java.io.*;
import java.util.*;

public class MixinEngine implements IMixinEngine {
    static Func<String, byte[]> bytecodeProvider;
    static boolean enableLog = false;
    static String id = "unknown";
    static Map<String, String> config = new HashMap<>();
    static Map<String, Object> property = new HashMap<>();
    static IMixinTransformer transformer;
    static IConsumer<MixinEnvironment.Phase> phaseConsumer;
    static final Object transformerLock = new Object();

    private static final MixinEngine instance = new MixinEngine();
    private static boolean started = false;

    private MixinEngine() {}

    public static MixinEngine getInstance() {
        return instance;
    }

    @Override
    public void setBytecodeProvider(Func<String, byte[]> bytecodeProvider) {
        MixinEngine.bytecodeProvider = bytecodeProvider;
    }

    @Override
    public void addConfig(String config, String id) {
        MixinEngine.config.put(id, config);
        if (started)
            Mixins.addConfiguration("copper://" + id + ".json");
    }

    @Override
    public void bootstrap() {
        MixinBootstrap.init();
        phaseConsumer.accept(MixinEnvironment.Phase.INIT);
        phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
        for (String id : MixinEngine.config.keySet())
            Mixins.addConfiguration("copper://" + id + ".json");
        started = true;
    }

    @Override
    public byte[] transform(String name, byte[] code) {
        if (transformer == null) {
            Log.warn("mixin:" + id, "try to transform class when transformer is not available: " + name);
            return null;
        }
        synchronized (transformerLock) {
            byte[] transformed = transformer.transformClassBytes(name, name, code);
            if (transformed == code)
                return null;
            return transformed;
        }
    }

    @Override
    public void setLogEnabled(boolean value) {
        enableLog = value;
    }

    @Override
    public void setEngineId(String id) {
        MixinEngine.id = id;
        Constants.DEBUG_OUTPUT_DIR = new File(new File(Constants.DEBUG_OUTPUT_PATH), id.replace(':', '-'));
    }

    @Override
    public void setFlag(String flag) {
        MixinEnvironment.Option option = MixinEnvironment.Option.valueOf(flag);
        MixinEnvironment.getCurrentEnvironment().setOption(option, true);
    }
}
