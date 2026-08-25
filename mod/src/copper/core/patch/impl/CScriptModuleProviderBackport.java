package copper.core.patch.impl;

import mindustry.mod.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import java.net.*;

@Pseudo
@Mixin(targets = "mindustry.mod.Scripts$ScriptModuleProvider")
public abstract class CScriptModuleProviderBackport {
    @Final
    @Shadow
    private Scripts this$0;

    @Redirect(method = "loadSource(Ljava/lang/String;Larc/files/Fi;Ljava/lang/Object;)Lrhino/module/provider/ModuleSource;",
        at = @At(value = "NEW", target = "Ljava/net/URI;"))
    private URI cInjectModNameToModuleUri(String uri) throws URISyntaxException {
        return new URI(((ICScriptsAccessor) this$0).getCurrentMod().name + "/" + uri + ".js");
    }
}
