package copper.core.mod;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import copper.core.util.*;
import mindustry.*;
import mindustry.mod.*;
import copper.loader.mod.Mod;

/**
 * Bridges a Copper {@link Mod} into Mindustry's {@link Mods.LoadedMod} system.
 */
public class LoadedCopperMod extends Mods.LoadedMod {
    private static Json json = new Json();
    public Mod copperMod;

    private LoadedCopperMod(Mod mod, Fi root, Mods.ModMeta meta) {
        super(new Fi(mod.file), root, (mindustry.mod.Mod) mod.instance, mod.container.getPublicClassLoader(), meta);
        if (!CopperMod.class.isAssignableFrom(main.getClass()))
            throw new ArcRuntimeException("mod main class is not a sub-class of CopperMod: " + mod.id);
    }

    /**
     * Builds a {@link LoadedCopperMod} from a Copper mod descriptor.
     */
    public static LoadedCopperMod build(Mod mod) {
        CopperModMeta meta = json.fromJson(CopperModMeta.class, mod.extraMeta);
        meta.copperMod = mod;
        meta.name = "copper-" + mod.id.replace(':', '-');
        meta.displayName = mod.name;
        meta.author = mod.author;
        meta.description = mod.description;
        meta.version = mod.version.toString();
        meta.main = mod.main;
        meta.repo = mod.repo;
        meta.hidden = mod.hidden;
        meta.java = true;
        meta.minGameVersion = String.valueOf(Vars.minJavaModGameVersion);

        if (!mod.id.equals("copper:core")) {
            Seq<String> dependencies = Seq.with("copper-copper-core");
            for (var desc : mod.dependency) {
                String name = desc.id;
                if (desc.id.equals("mindustry") || desc.id.equals("loader"))
                    continue;
                else if (desc.id.equals("copper:core"))
                    continue;
                else if (desc.id.startsWith("mindustry:"))
                    name = name.substring(10);
                else
                    name = "copper-" + name.replace(':', '-');
                dependencies.add(name);
            }
            meta.dependencies = dependencies;
        }
        meta.cleanup();

        Fi root = new Fi(mod.file);
        if (!root.isDirectory())
            root = new ZipFi(root);
        return new LoadedCopperMod(mod, root.child("assets"), meta);
    }

    @Override
    public boolean shouldBeEnabled() {
        return true;
    }
}
