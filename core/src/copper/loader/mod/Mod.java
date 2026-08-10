package copper.loader.mod;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.meta.*;
import copper.loader.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

public class Mod {
    private static final IMetaReader[] metaReaders = new IMetaReader[] {new MetaReaderV1()};

    public String id;
    public String author;
    public String name;
    public String description;
    public boolean hidden;
    public Version version;
    public String main;
    public String repo;
    public String extraMeta;

    public ArrayList<ModDescriptor> dependency;
    public ArrayList<ModDescriptor> conflict;
    public ArrayList<MixinDescriptor> mixin;
    public ArrayList<String> exportRule;
    public Map<String, ArrayList<String>> importRule;

    public File file;
    public Container container;
    public Object instance;

    public Mod(File baseFile) {
        id = author = name = description = main = repo = "";
        extraMeta = "{}";
        hidden = false;
        dependency = new ArrayList<>();
        conflict = new ArrayList<>();
        mixin = new ArrayList<>();
        exportRule = new ArrayList<>();
        importRule = new HashMap<>();
        try {
            file = baseFile;
            container = Loader.platform.createModContainer(file);
            loadMeta();
            container.id = id;
        } catch (Throwable e) {
            throw new RuntimeException("failed to read mod: " + baseFile.getName(), e);
        }
    }

    protected void loadMeta(){
        try {
            byte[] metaContent = container.resource.get("copper.mod.json");
            if (metaContent == null)
                metaContent = container.resource.get("copper.mod.hjson");
            if (metaContent == null)
                throw new RuntimeException("failed to find mod meta file in mod jar");
            String metaJson = new String(metaContent, StandardCharsets.UTF_8);
            Jval meta = Jval.read(metaJson);

            int ver = meta.getInt("version", 0);
            if (ver <= 0 || ver > metaReaders.length)
                throw new RuntimeException("meta version is not supported: " + ver);
            metaReaders[ver - 1].read(this, meta.get("meta"));
        } catch (Throwable e) {
            throw new RuntimeException("failed to load mod meta: " + file.getName(), e);
        }
    }

    public void preInit() {
        try {
            container.init();
        } catch (Exception e) {
            throw new RuntimeException("failed to pre-init mod: " + id, e);
        }
    }

    public void init() {
        try {
            Class<?> main = container.loadPublicOwnClass(this.main);
            if (main == null)
                throw new RuntimeException("main class is not found: " + this.main);
            Method init = main.getMethod("init");
            if (Modifier.isStatic(init.getModifiers()))
                init.invoke(null);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("failed to init mod: " + id, e);
        }
    }

    public void load() {
        if (instance != null)
            return;
        try {
            Class<?> main = container.loadOwnClass(this.main);
            if (main == null)
                throw new RuntimeException("main class is not found: " + this.main);
            instance = main.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("failed to load mod: " + id, e);
        }
    }

    public void resolve() {
        for (String rule : exportRule)
            container.export.addRule(rule);
        container.export.addRule("include " + id.replace(':', '.') + ".*");

        for (ModDescriptor dep : dependency) {
            if (dep.id.equals("mindustry")) {
                if (!dep.version.check(Loader.game.version))
                    throw new RuntimeException("game version is rejected by " + id + " : " + Loader.game.version.toString());
            } else if (dep.id.equals("loader")) {
                if (!dep.version.check(Loader.vars.loaderVersion))
                    throw new RuntimeException("loader version is rejected by " + id + " : " + Loader.vars.loaderVersion.toString());
            } else {
                Mod o = Loader.mods.getModById(dep.id);
                if (o == null)
                    throw new RuntimeException("failed to find dependency for " + id + " : " + dep.id);
                if (!dep.version.check(o.version))
                    throw new RuntimeException("dependency is not supported by " + id + " : " + o.id + " " + o.version.toString());
                // core mod will be added in Mods::resolveMod
                if (!o.id.equals("copper:core")) {
                    DependencyInfo info = new DependencyInfo(o.container);
                    for (String rule : importRule.get(o.id))
                        info.extraImport.addRule(rule);
                    container.dependency.add(info);
                }
            }
        }

        for (ModDescriptor con : conflict) {
            if (con.id.equals("mindustry")) {
                if (con.version.check(Loader.game.version))
                    throw new RuntimeException("game version is rejected by " + id + " : " + Loader.game.version.toString());
            } else if (con.id.equals("loader")) {
                if (con.version.check(Loader.vars.loaderVersion))
                    throw new RuntimeException("loader version is rejected by " + id + " : " + Loader.vars.loaderVersion.toString());
            } else {
                Mod o = Loader.mods.getModById(con.id);
                if (o == null)
                    continue;
                if (con.version.check(o.version))
                    throw new RuntimeException("mod is conflict with " + id + " : " + o.id + " " + o.version.toString());
            }
        }

        for (var mixin : mixin) {
            Container target = null;
            Version version = null;
            if (mixin.id.equals("mindustry")) {
                target = Loader.game.container;
                version = Loader.game.version;
            } else {
                var mod = Loader.mods.getModById(mixin.id);
                if (mod != null) {
                    target = mod.container;
                    version = mod.version;
                }
            }
            if (target == null) {
                Log.warn("Failed to find mixin target for " + id + " : " + mixin.id);
                continue;
            }
            if (!mixin.version.check(version))
                continue;
            Log.debug(null, "Selected mod mixin config: %s -> %s : %s", id, mixin.id, mixin.configPath);

            byte[] txtBytes = container.resource.get("assets/copper/" + mixin.configPath);
            if (txtBytes == null)
                throw new RuntimeException("failed to find mixin config in copper assets of mod " + id + " : " + mixin.configPath);
            MixinInfo info = new MixinInfo(container, new String(txtBytes, StandardCharsets.UTF_8));
            target.mixin.add(info);
        }
    }
}
