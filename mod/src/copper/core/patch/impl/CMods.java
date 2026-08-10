package copper.core.patch.impl;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import copper.core.*;
import copper.core.mod.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Mods.class)
public abstract class CMods {
    @Shadow
    Seq<Mods.LoadedMod> mods;
    @Shadow
    ObjectMap<Class<?>, Mods.ModMeta> metas;

    @Inject(method = "load", at = @At(value = "INVOKE", target = "sortMods"))
    void cLoadCopperMod(CallbackInfo ci) {
        Log.info("Loading copper mods.");
        Loader.mods.load();
        Seq<LoadedCopperMod> loadedMods = new Seq<>();
        Loader.mods.eachMod(mod -> {
            if (!(mod instanceof MindustryMod)) {
                LoadedCopperMod loaded = LoadedCopperMod.build(mod);
                metas.put(loaded.main.getClass(), loaded.meta);
                loadedMods.add(loaded);
            }
        });
        mods.addAll(loadedMods);

        loadedMods.each(this::updateDependencies);
        for(var mod : loadedMods){
            // skip mods where the state has already been resolved
            if(mod.state != Mods.ModState.enabled)
                continue;
            if(!mod.isSupported())
                mod.state = Mods.ModState.unsupported;
        }
        Log.info("Loaded @ copper mods.", loadedMods.count(Mods.LoadedMod::enabled));
    }

    @Inject(method = "load", at = @At("RETURN"))
    void cLoadRegisterExtendedPackets(CallbackInfo ci) {
        Vars.mods.eachEnabled(mod -> {
            if (mod.main instanceof CopperMod copperMod)
                copperMod.registerPackets();
        });
    }

    @Inject(method = "removeMod", at = @At("HEAD"), cancellable = true)
    void cBanRemoveMod(Mods.LoadedMod mod, CallbackInfo ci) {
        Vars.ui.showErrorMessage(CoreMod.bundles.get("notice.mod.remove"));
        ci.cancel();
    }

    @Inject(method = "setEnabled", at = @At("HEAD"), cancellable = true)
    void cBanSetModEnabled(Mods.LoadedMod mod, boolean enabled, CallbackInfo ci) {
        Vars.ui.showErrorMessage(CoreMod.bundles.get("notice.mod.set-enable"));
        ci.cancel();
    }

    @Inject(method = "importMod(Larc/files/Fi;Z)Lmindustry/mod/Mods$LoadedMod;", at = @At("HEAD"), cancellable = true)
    void cBanImportMod(Fi file, boolean forceEnable, CallbackInfoReturnable<Mods.LoadedMod> ci) {
        Vars.ui.showErrorMessage(CoreMod.bundles.get("notice.mod.import"));
        Mods.ModMeta fakeMeta = new Mods.ModMeta();
        fakeMeta.name = file.nameWithoutExtension();
        fakeMeta.cleanup();
        Mods.LoadedMod fake = new Mods.LoadedMod(file, file, null, null, fakeMeta) {
            @Override
            public void setRepo(String repo) {}
        };
        ci.setReturnValue(fake);
    }

    @Inject(method = "getConfigFolder", at = @At("HEAD"), cancellable = true)
    void cReplaceCopperConfigFolder(Mod mod, CallbackInfoReturnable<Fi> ci) {
        Mods.ModMeta meta = metas.get(mod.getClass());
        if (meta instanceof CopperModMeta copper)
            ci.setReturnValue(Copper.getModsDataFolder().child(copper.copperMod.id.replace(':', '-')));
    }

    @Inject(method = "skipModLoading", at = @At("RETURN"), cancellable = true)
    void cForceModLoad(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }

    @Shadow
    abstract void updateDependencies(Mods.LoadedMod mod);
}
