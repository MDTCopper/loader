package copper.core.patch.impl;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import copper.core.*;
import copper.core.mod.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: integrates Copper mods into Mindustry's mod loading pipeline.
 *
 * <p>This is the main integration point: it hooks into {@code Mods.load()} to
 * add Copper mods alongside regular ones, prevents in-game mod management
 * (removal, enabling/disabling, importing), redirects config folders, and
 * forces mod loading. See individual injector methods for details.</p>
 */
@Mixin(Mods.class)
public abstract class CMods {
    @Shadow
    private Seq<Mods.LoadedMod> mods;
    @Shadow
    private ObjectMap<Class<?>, Mods.ModMeta> metas;
    @Shadow
    private Seq<Mods.LoadedMod> lastOrderedMods;

    /**
     * Loads Copper mods and injects them into Mindustry's mod list.
     * Injects before {@code sortMods()} to ensure correct ordering.
     */
    @Inject(method = "load", at = @At(value = "INVOKE", target = "sortMods"))
    private void cLoadCopperMod(CallbackInfo ci) {
        Log.info("Loading copper mods.");
        Loader.mods.load();
        Seq<LoadedCopperMod> loadedMods = new Seq<>();
        Loader.mods.eachMod(mod -> {
            if (!(mod instanceof MindustryMod)) {
                LoadedCopperMod loaded = LoadedCopperMod.build(mod);
                metas.put(loaded.main.getClass(), loaded.meta);
                loadedMods.add(loaded);
                // invalidate ordered mods cache
                lastOrderedMods = null;
            }
        });
        mods.addAll(loadedMods);

        loadedMods.each(this::updateDependencies);
        Log.info("Loaded @ copper mods.", loadedMods.count(Mods.LoadedMod::enabled));
    }

    /** Registers Copper extended packets for all enabled Copper mods after loading. */
    @Inject(method = "load", at = @At("RETURN"))
    private void cLoadRegisterExtendedPackets(CallbackInfo ci) {
        if (!Loader.vars.vanillaMode) {
            Vars.mods.eachEnabled(mod -> {
                if (mod.main instanceof CopperMod copperMod)
                    copperMod.registerPackets();
            });
        }
    }

    /** Prevents mod removal from the in-game UI. */
    @Inject(method = "removeMod", at = @At("HEAD"), cancellable = true)
    private void cBanRemoveMod(Mods.LoadedMod mod, CallbackInfo ci) {
        Vars.ui.showErrorMessage(CoreMod.bundles.get("notice.mod.remove"));
        ci.cancel();
    }

    /** Prevents enabling/disabling mods from the in-game UI. */
    @Inject(method = "setEnabled", at = @At("HEAD"), cancellable = true)
    private void cBanSetModEnabled(Mods.LoadedMod mod, boolean enabled, CallbackInfo ci) {
        Vars.ui.showErrorMessage(CoreMod.bundles.get("notice.mod.set-enable"));
        ci.cancel();
    }

    /** Prevents importing mods from the in-game UI; returns a fake loaded mod instead. */
    @Inject(method = "importMod(Larc/files/Fi;Z)Lmindustry/mod/Mods$LoadedMod;", at = @At("HEAD"), cancellable = true)
    private void cBanImportMod(Fi file, boolean forceEnable, CallbackInfoReturnable<Mods.LoadedMod> ci) {
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

    /** Redirects the config folder for Copper mods to the Copper data directory. */
    @Inject(method = "getConfigFolder", at = @At("HEAD"), cancellable = true)
    private void cReplaceCopperConfigFolder(Mod mod, CallbackInfoReturnable<Fi> ci) {
        Mods.ModMeta meta = metas.get(mod.getClass());
        if (meta instanceof CopperModMeta copper)
            ci.setReturnValue(Copper.getModsDataFolder().child(copper.copperMod.id.replace(':', '-')));
    }

    /** Forces mod loading (overrides the {@code skipModLoading} setting). */
    @Inject(method = "skipModLoading", at = @At("RETURN"), cancellable = true)
    private void cForceModLoad(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }

    /**
     * Replaces {@code Platform.loadJar()} when a Mindustry Java mod is loaded:
     * Copper-managed mods use the Copper container classloader instead, so their
     * dependencies and mixins work; anything else falls back to the vanilla path.
     */
    @Redirect(method = "loadMod(Larc/files/Fi;ZZ)Lmindustry/mod/Mods$LoadedMod;", at = @At(value = "INVOKE", target = "Lmindustry/core/Platform;loadJar(Larc/files/Fi;Ljava/lang/ClassLoader;)Ljava/lang/ClassLoader;"))
    private ClassLoader cLoadMdtModJar(Platform platform, Fi jar, ClassLoader loader) {
        copper.loader.mod.Mod mod = Loader.mods.getModByFile(jar.file());
        if (mod instanceof MindustryMod && !mod.main.isEmpty())
            return mod.container.getClassLoader();
        try {
            return platform.loadJar(jar, loader);
        } catch (Throwable e) {
            throw new ArcRuntimeException(e);
        }
    }

    @Shadow
    abstract void updateDependencies(Mods.LoadedMod mod);
}
