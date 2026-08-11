package copper.core.patch.impl;

import arc.*;
import arc.files.*;
import copper.loader.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "mindustry.server.ServerLauncher")
public abstract class CServerLauncher {
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Larc/Settings;setDataDirectory(Larc/files/Fi;)V"))
    private void cReplaceDataFolder(Settings settings, Fi folder) {
        settings.setDataDirectory(new Fi(Loader.vars.gameDataFolder));
    }
}
