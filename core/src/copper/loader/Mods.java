package copper.loader;

import copper.loader.container.info.*;
import copper.loader.func.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;

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

    public Mod getModById(String id) {
        return mod.get(id);
    }

    public Mod getModByFile(File file) {
        return pathMap.get(file.getAbsolutePath());
    }

    public void eachMod(Cons<Mod> cons) {
        for (Mod m : orderedMod)
            cons.get(m);
    }

    public List<Mod> getMods() {
        return orderedMod;
    }

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

    private void sortMod() {
        orderedMod.clear();

        var mods = mod.values().toArray(Mod[]::new);
        for (Mod m : mods)
            if (m instanceof MindustryMod)
                orderedMod.add(m);

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
            inDegCnt.put(m, m.dependency.size());
            for (var info : m.dependency) {
                if (!info.id.equals("mindustry") && !info.id.equals("loader"))
                    outDeg.computeIfAbsent(mod.get(info.id), k -> new ArrayList<>())
                            .add(m);
                else
                    inDegCnt.computeIfPresent(m, (k, v) -> v - 1);
            }
        }
        sort.get(orderedMod, "mod dependency path");
        // core mod should be loaded first to register all copper mods into mindustry
        Structs.swap(orderedMod, 0, orderedMod.indexOf(mod.get("copper:core")));

        inDegCnt.clear();
        outDeg.clear();
        for (Mod m : mods) {
            inDegCnt.put(m, m.mixin.size());
            for (var mixin : m.mixin) {
                if (!mixin.id.equals("mindustry") && !mixin.id.equals("loader"))
                    outDeg.computeIfAbsent(mod.get(mixin.id), k -> new ArrayList<>())
                            .add(m);
                else
                    inDegCnt.put(m, m.mixin.size() - 1);
            }
        }
        sort.get(new ArrayList<>(), "mod mixin path");
    }

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

    public void preInit() {
        eachMod(Mod::preInit);
    }

    public void init() {
        eachMod(Mod::init);
    }

    public void load() {
        eachMod(Mod::load);
    }
}
