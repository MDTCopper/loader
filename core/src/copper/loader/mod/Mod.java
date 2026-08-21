package copper.loader.mod;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.meta.*;
import copper.loader.mod.mixin.*;
import copper.loader.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

/**
 * A Copper-native mod, backed by a {@link MixinContainer} and described by a
 * {@code copper.mod.json} (or {@code .hjson}) meta file.
 *
 * <p>Lifecycle:<br>
 * construction reads the meta file and creates the container;<br>
 * {@link #resolve()} wires dependencies, conflicts, export rules, and mixin configs
 * (called during {@link Mods#read});<br>
 * {@link #init()} initializes the container (classloader + mixin);<br>
 * {@link #bootstrap()} invokes the mod's static {@code bootstrap()} method;<br>
 * {@link #load()} instantiates the main class.</p>
 */
public class Mod {
    /** Registered meta readers, indexed by version number minus 1. */
    private static final IMetaReader[] metaReaders = new IMetaReader[] {new MetaReaderV1()};
    /** Registered mixin config readers, indexed by version number minus 1. */
    private static final IMixinConfigReader[] mixinReaders = new IMixinConfigReader[] {new MixinConfigReaderV1()};

    /** Unique mod identifier ({@code author:name}). */
    public String id;
    /** Author name displayed in game */
    public String author;
    /** Mod name displayed in game */
    public String name;
    /** Mod description displayed in game */
    public String description;
    /** Whether this mod is hidden (server/client-side only, no new content). Passed through to the game's mod meta. */
    public boolean hidden;
    /** The mod's semantic version. */
    public Version version;
    /** Fully qualified main class name. */
    public String main;
    /** URL to the mod's repository or homepage. */
    public String repo;
    /** Additional ModMeta fields (e.g. subtitle), stored as a plain JSON string. */
    public String extraMeta;

    /** Declared dependency descriptors (includes conflicts). */
    public List<ModDescriptor> dependency;
    /** Declared conflict descriptors. */
    public List<ModDescriptor> conflict;
    /** Mixin configuration descriptors. */
    public List<MixinDescriptor> mixin;
    /** Own-class export rules ({@code include/exclude <pattern>}). */
    public List<String> exportRule;
    /** Per-dependency import rules ({@code depId → ["include ...", ...]}). */
    public Map<String, List<String>> importRule;

    /** The mod file on disk (jar or directory). */
    public File file;
    /** The class/resource container for this mod. */
    public MixinContainer container;
    /** The instantiated main class instance (set after {@link #load()}). */
    public Object instance;

    /**
     * Creates a mod by reading its jar/directory and parsing its meta file.
     *
     * @param baseFile the mod file (jar/zip or directory)
     */
    Mod(File baseFile) {
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

    /**
     * Loads and parses the mod's meta file ({@code copper.mod.json} or {@code copper.mod.hjson}).
     *
     * <p>The meta file must have a {@code "version"} field and a {@code "meta"} object.
     * The version number selects the appropriate {@link IMetaReader}.</p>
     */
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

    /**
     * Initializes the mod container (classloader, mixin engine).
     */
    void init() {
        try {
            container.init();
        } catch (Exception e) {
            throw new RuntimeException("failed to init mod: " + id, e);
        }
    }

    /**
     * Invokes the mod main class's static {@code init()} method, if present.
     */
    void bootstrap() {
        try {
            Class<?> main = container.loadPublicOwnClass(this.main);
            if (main == null)
                throw new RuntimeException("main class is not found: " + this.main);
            Method init = main.getMethod("bootstrap");
            int modifiers = init.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
                init.invoke(null);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("failed to bootstrap mod: " + id, e);
        }
    }

    /**
     * Instantiates the mod main class via its no-arg constructor.
     */
    void load() {
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

    /**
     * Validates dependencies and conflicts, wires export/import rules, and registers mixin configs.
     * The containers of core mod, game and loader are added in `Mods.resolveMod` before calling this.
     */
    void resolve() {
        for (String rule : exportRule)
            container.export.addRule(rule);

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
                // The core mod is added separately in Mods.resolveMod.
                if (!o.id.equals("copper:core")) {
                    DependencyInfo info = new DependencyInfo(o.container);
                    if (importRule.containsKey(o.id)) {
                        for (String rule : importRule.get(o.id))
                            info.extraImport.addRule(rule);
                    }
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
            MixinContainer target = null;
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

            byte[] txtBytes = container.resource.get("assets/copper/" + mixin.configPath);
            if (txtBytes == null)
                throw new RuntimeException("failed to find mixin config in copper assets of mod " + id + " : " + mixin.configPath);
            try {
                Jval config = Jval.read(new String(txtBytes, StandardCharsets.UTF_8));
                int ver = config.getInt("version", 0);
                if (ver <= 0 || ver > mixinReaders.length)
                    throw new RuntimeException("mixin config version is not supported: " + ver);
                MixinInfo info = mixinReaders[ver - 1].read(container, version, config.get("config"));
                target.mixin.add(info);
            } catch (Throwable e) {
                throw new RuntimeException("failed to read mixin config in mod " + id + " : " + mixin.configPath);
            }
        }
    }
}
