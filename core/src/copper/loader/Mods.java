package copper.loader;

import copper.loader.container.info.*;
import copper.loader.func.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;

/**
 * Manages mod discovery, topological ordering, dependency resolution,
 * and the mod lifecycle (read → pre-init → init → load).
 *
 * <p>Mods are scanned from two directories (Copper-native and standard Mindustry)
 * and sorted so that dependencies load before dependents. The Copper core mod is
 * always placed first.</p>
 */
public class Mods {
    private static final String[] mindustryMetaFiles
            = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"};
    private static final String[] copperMetaFiles
            = {"copper.mod.json", "copper.mod.hjson"};

    private Map<String, Mod> mod;
    private ArrayList<Mod> orderedMod;
    private Map<String, Mod> pathMap;

    public Mods() {
        mod = new HashMap<>();
        orderedMod = new ArrayList<>();
        pathMap = new HashMap<>();
    }

    /**
     * Looks up a mod by its string id (e.g., {@code "copper:core"}).
     */
    public Mod getModById(String id) {
        return mod.get(id);
    }

    /**
     * Looks up a mod by its file path on disk.
     */
    public Mod getModByFile(File file) {
        return pathMap.get(file.getAbsolutePath());
    }

    /**
     * Iterates over all mods in load order.
     */
    public void eachMod(Cons<Mod> cons) {
        for (Mod m : orderedMod)
            cons.get(m);
    }

    /**
     * Returns the mod list in load order.
     */
    public List<Mod> getMods() {
        return orderedMod;
    }

    /**
     * Scans a folder for mod files and reads their metadata.
     *
     * @param folder      the folder to scan
     * @param metaFiles   candidate meta file names inside each mod
     * @param constructor factory that creates a {@link Mod} from a {@link File}
     */
    private void readMod(File folder, String[] metaFiles, Func<File, Mod> constructor) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (!fileName.endsWith(".jar") && !fileName.endsWith(".zip"))
                    continue;
                if (file.isDirectory() && !Structs.contains(metaFiles, name -> (new File(file, name)).exists()))
                    continue;
                try {
                    Mod m = constructor.get(file);
                    if (mod.containsKey(m.id))
                        throw new RuntimeException("found duplicated mod: " + m.id);
                    mod.put(m.id, m);
                    pathMap.put(file.getAbsolutePath(), m);
                } catch (Throwable e) {
                    Log.error("failed to read mod: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Topologically sorts mods by their dependency and mixin dependency graphs,
     * reporting cycles as errors.
     */
    private void sortMod() {
        orderedMod.clear();

        var mods = mod.values().toArray(Mod[]::new);
        for (Mod m : mods)
            if (m instanceof MindustryMod)
                orderedMod.add(m);
        int mdtModCnt = orderedMod.size();

        Map<Mod, Integer> inDegCnt = new HashMap<>();
        Map<Mod, ArrayList<Mod>> outDeg = new HashMap<>();
        Cons2<ArrayList<Mod>, String> sort = (list, type) -> {
            int lastSorted = 0, sorted = 0;
            while (true) {
                for (var entry : inDegCnt.entrySet()) {
                    if (entry.getValue() == 0) {
                        list.add(entry.getKey());
                        sorted ++;
                        entry.setValue(-1);
                    }
                }
                if (lastSorted == sorted)
                    break;
                for (int i = lastSorted; i < sorted; i++) {
                    var l = outDeg.get(mods[i]);
                    if (l != null) {
                        for (Mod j : l)
                            inDegCnt.computeIfPresent(j, (k, v) -> v - 1);
                    }
                }
                lastSorted = sorted;
            }
            if (lastSorted != inDegCnt.size()) {
                StringBuilder msg = new StringBuilder("one or more rings were found in " + type + ", related mods:");
                for (var entry : inDegCnt.entrySet())
                    if (entry.getValue() > 0)
                        msg.append(" ").append(entry.getKey().id);
                throw new RuntimeException(msg.toString());
            }
        };

        for (Mod m : mods) {
            if (m instanceof MindustryMod)
                continue;
            inDegCnt.put(m, m.dependency.size());
            for (var info : m.dependency) {
                if (!info.id.equals("mindustry") && !info.id.equals("loader") && !info.id.startsWith("mindustry:"))
                    outDeg.computeIfAbsent(mod.get(info.id), k -> new ArrayList<>())
                            .add(m);
                else
                    inDegCnt.computeIfPresent(m, (k, v) -> v - 1);
            }
        }
        sort.get(orderedMod, "mod dependency path");

        // The core mod must load first so it can register all Copper mods into Mindustry.
        Structs.swap(orderedMod, mdtModCnt, orderedMod.indexOf(mod.get("copper:core")));

        inDegCnt.clear();
        outDeg.clear();
        for (Mod m : mods) {
            if (m instanceof MindustryMod)
                continue;
            inDegCnt.put(m, m.mixin.size());
            for (var mixin : m.mixin) {
                if (!mixin.id.equals("mindustry") && !mixin.id.equals("loader") && !mixin.id.startsWith("mindustry:"))
                    outDeg.computeIfAbsent(mod.get(mixin.id), k -> new ArrayList<>())
                            .add(m);
                else
                    inDegCnt.computeIfPresent(m, (k, v) -> v - 1);
            }
        }
        sort.get(new ArrayList<>(), "mod mixin path");
    }

    /**
     * Wires up container dependencies: every mod gets the Copper core mod,
     * the game, and the loader as dependencies.
     */
    private void resolveMod() {
        DependencyInfo coreInfo = new DependencyInfo(mod.get("copper:core").container);
        DependencyInfo gameInfo = new DependencyInfo(Loader.game.container);
        DependencyInfo loaderInfo = new DependencyInfo(Loader.vars.loaderContainer);
        eachMod(m -> {
            if (!m.id.equals("copper:core"))
                m.container.dependency.add(coreInfo);
        });
        eachMod(m -> m.container.dependency.add(gameInfo));
        eachMod(m -> m.container.dependency.add(loaderInfo));
        eachMod(Mod::resolve);
    }

    /**
     * Discovers all mods, sorts them by dependency order, and resolves dependencies.
     */
    public void read() {
        Loader.platform.extractCoreMod();
        readMod(Loader.vars.copperModFolder, copperMetaFiles, Mod::new);
        readMod(Loader.vars.gameModFolder, mindustryMetaFiles, MindustryMod::new);

        Mod core = mod.get("copper:core");
        if (core == null)
            throw new RuntimeException("core mod is not found");
        core.version = Loader.vars.loaderVersion;

        sortMod();
        resolveMod();

        Log.info("Found " + mod.size() + " mods.");
        if (Log.getLevel() == Log.Level.DEBUG) {
            Log.debug("Mods list: ");
            eachMod(mod -> Log.debug("  -> " + mod.id + " " + mod.version.toString()));
        }
    }

    /**
     * Calls {@link Mod#preInit()} on every mod in load order.
     */
    public void preInit() {
        eachMod(Mod::preInit);
    }

    /**
     * Calls {@link Mod#init()} on every mod in load order.
     */
    public void init() {
        eachMod(Mod::init);
    }

    /**
     * Calls {@link Mod#load()} on every mod in load order.
     */
    public void load() {
        eachMod(Mod::load);
    }
}
