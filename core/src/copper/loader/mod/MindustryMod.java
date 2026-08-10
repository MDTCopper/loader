package copper.loader.mod;

import copper.loader.*;
import copper.loader.container.info.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class MindustryMod extends Mod {
    private static final String[] metaFiles
            = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"};
    private static final Map<String, DependencyInfo> dependencyInfoCache = new HashMap<>();

    public MindustryMod(File baseFile) {
        super(baseFile);
    }

    @Override
    protected void loadMeta() {
        try {
            byte[] metaContent = null;
            for (String name : metaFiles) {
                metaContent = container.resource.get(name);
                if (metaContent != null)
                    break;
            }
            if (metaContent == null)
                throw new RuntimeException("failed to find mod meta file in mod jar");
            String metaJson = new String(metaContent, StandardCharsets.UTF_8);
            Jval.JsonMap meta = Jval.read(metaJson).asObject();

            // necessary info

            name = meta.get("name").asString().trim();
            id = "mindustry:" + name;

            // optional info

            if (meta.containsKey("author"))
                author = meta.get("author").asString();
            if (meta.containsKey("displayName"))
                name = meta.get("displayName").asString();
            if (meta.containsKey("description"))
                description = meta.get("description").asString();
            if (meta.containsKey("hidden"))
                hidden = meta.get("hidden").asBool();
            if (meta.containsKey("repo"))
                repo = meta.get("repo").asString();

            if (meta.containsKey("version")) {
                String ver = meta.get("version").asString();
                try {
                    version = new SemanticVersion(ver);
                } catch (Throwable e) {
                    version = new StringVersion(ver);
                }
            } else {
                version = new StringVersion("");
            }

            if (meta.containsKey("main")) {
                main = meta.get("main").asString();
                if (container.resource.get(main.replace('.', '/') + ".class") == null)
                    main = "";
            }

        } catch (Throwable e) {
            throw new RuntimeException("failed to load mod meta: " + file.getName(), e);
        }
    }

    @Override
    public void init() {
        try {
            container.init();
        } catch (Throwable e) {
            throw new RuntimeException("failed to init mod: " + id, e);
        }
    }

    @Override
    public void load() {}

    @Override
    public void resolve() {
        // the same behaviour with mindustry.mod.ModClassLoader
        Loader.mods.eachMod(m -> {
            if (!(m instanceof MindustryMod) || m.id.equals(this.id))
                return;
            DependencyInfo info = dependencyInfoCache.get(m.id);
            if (info == null) {
                info = new DependencyInfo();
                info.container = m.container;
                info.extraImport.addRule("include *");
                dependencyInfoCache.put(m.id, info);
            }
            container.dependency.add(info);
        });
    }
}
