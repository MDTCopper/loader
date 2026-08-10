package copper.loader.mixin;

import copper.loader.util.*;
import org.spongepowered.asm.service.*;

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
