package copper.loader.mixin;

import copper.loader.util.*;
import org.spongepowered.asm.service.*;

/**
 * Bootstrap entry point registered via the Mixin service loader.
 * Delegates to {@link MixinEngineService}.
 */
public class MixinEngineBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "Copper";
    }

    @Override
    public String getServiceClassName() {
        return "copper.loader.mixin.MixinEngineService";
    }

    @Override
    public void bootstrap() {
        Log.info("mixin:" + MixinEngine.id, "Copper mixin engine bootstrap.");
    }
}
