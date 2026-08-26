package copper.launch;

import copper.launch.asm.*;
import copper.launch.builder.*;
import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.func.*;
import copper.loader.mod.Version;
import copper.loader.util.*;
import org.objectweb.asm.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/**
 * Builds the ART (Android) runtime cache from the desktop game jar.
 *
 * <p>It runs either on a desktop JVM (the built {@code -desktop.jar}) or directly
 * on the device (the launcher app's "build" action); see {@link ArtBuilderPlatform}.
 * The pipeline is roughly:</p>
 * <ol>
 *   <li>{@code --init}: compile the android components, pack game assets and native libs.</li>
 *   <li>{@code --build}: load every enabled mod, apply their mixins to the bytecode,
 *       dex each mod with d8, and merge the results into per-mod mixin dex jars;
 *       a runtime meta then maps every mod to its jar.</li>
 * </ol>
 *
 * <p>The result is a cache folder that {@link ArtLauncher} later loads on the device,
 * where mixins are already applied and no runtime transformation is needed.</p>
 */
public class ArtBuilder {
    private static DexCache dexCache;
    private static ZipFile gameJar;
    private static ZipFile loaderJar;
    private static ZipFile gameSrc;
    private static ZipFile arcSrc;

    /** Map of mod id to resource; entries are filled lazily (see {@link #loadResource}). */
    private static Map<String, D8Resource> resourceMap = new LinkedHashMap<>();
    /** System libraries every dex compile needs (android.jar, desugar libs, ...). */
    private static List<D8Resource> systemResource = new ArrayList<>();
    /** In-memory base dex pools, only kept on the desktop to save heap. */
    private static Map<String, BaseDexPool> baseDexPoolMap = new HashMap<>();
    /** Map of mod id to its mixin dex metadata, built in {@link #buildMixinDex()}. */
    private static Map<String, MixinDexMeta> mixinDexMetaMap = new HashMap<>();
    /** Map of mod id to the class filter selecting its mixin-transformed classes. */
    private static Map<String, ClassFilter> deltaSourceFilter = new HashMap<>();
    /** Whether the loader environment has been booted for mixin application. */
    private static boolean loaderLaunched = false;
    /** The d8 desugared-lib config json, read from an internal jar. */
    private static String desugarConfig;

    public static void main(String[] args) {
        if (System.console() == null)
            Log.setBackend(Log.Backend::colorless);

        ArgParser parser = new ArgParser("CopperArtBuilder", "A builder to build art runtime cache used by copper loader.");
        parser.addOption("G", "game-jar", "Game jar path (desktop version required)", "path", path -> ArtPlatform.gameFile = new File(path));
        parser.addOption("D", "game-data", "Game data folder path", "path", path -> ArtPlatform.gameDataFolder = new File(path));
        parser.addOption("L", "loader-jar", "Loader jar path", "path", path -> ArtPlatform.jarFile = new File(path));
        parser.addOption("C", "cache-path", "Cache path", "path", path -> ArtPlatform.cacheFolder = new File(path));
        parser.addOption(null, "game-src", "Game source code zip path", "path");
        parser.addOption(null, "arc-src", "Arc source code zip path", "path");
        parser.addFlag(null, "vanilla", "Process the vanilla game", () -> Loader.vars.vanillaMode = true);
        // for debugging
        parser.addFlag(null, "desktop", "Running on desktop", () -> ArtBuilderPlatform.desktopMode = true);
        parser.addFlag(null, "verbose", "Enable verbose log output", () -> Log.setLevel(Log.Level.VERBOSE));
        // operations
        parser.addFlag(null, "init", "Create and build the base cache");
        parser.addFlag(null, "build", "Build the cache");
        parser.addFlag(null, "clear", "Clear all built caches except base cache");
        parser.addOption(null, "remove", "Remove all related built caches", "modId");

        try {
            parser.parse(args);
            checkFileExists(ArtPlatform.gameFile, "Game jar file");
            checkFileProvided(ArtPlatform.gameDataFolder, "Game data folder");
            checkFileExists(ArtPlatform.jarFile, "Loader jar file");
            checkFileProvided(ArtPlatform.cacheFolder, "Cache folder");

            gameJar = new ZipFile(ArtPlatform.gameFile);
            loaderJar = new ZipFile(ArtPlatform.jarFile);

            ArtPlatform.init();
            Loader.platform = new ArtBuilderPlatform();
            Loader.vars.init();
            Log.setOutputFile(new File(ArtPlatform.cacheFolder, "last_log.txt"));
            dexCache = new DexCache(new File(ArtPlatform.cacheFolder, "dex"));

            Log.info("CopperArtBuilder v" + Loader.vars.loaderVersion.toString());

            if (parser.hasOption("clear")) {
                Log.info("Cleaning built cache.");
                dexCache.clear();
            }

            if (parser.hasOption("remove")) {
                for (String id : parser.getOptionValues("remove")) {
                    Log.info("Remove all built caches related to: " + id);
                    dexCache.remove(id);
                }
            }

            if (parser.hasOption("init")) {
                Log.info("Creating the base cache.");
                File gameSrcFile = new File(parser.getOptionValue("game-src"));
                File arcSrcFile = new File(parser.getOptionValue("arc-src"));
                checkFileExists(gameSrcFile, "Game source zip");
                checkFileExists(arcSrcFile, "Arc source zip");
                gameSrc = new ZipFile(gameSrcFile);
                arcSrc = new ZipFile(arcSrcFile);

                buildAndroidComponent();
                packGameAssets();
                packGameLibs();
            }

            if (parser.hasOption("build")) {
                Log.info("Building the cache.");
                Loader.init();
                dexCache.init();
                // skip everything if the dex for this exact mod set already exists
                if (!dexCache.isCurrentRuntimeExisted()) {
                    if (parser.hasOption("verbose"))
                        enableMixinLog();
                    loadResouceEntry();
                    buildBaseDexPool();
                    buildMixinDex();
                    buildRuntime();
                }
            }

            Log.info("Done.");
        } catch (Throwable e) {
            Log.error(e);
            throw new RuntimeException("failed to execute builder command", e);
        }
    }

    /**
     * Compiles the android sources of the game and the arc backend into
     * {@code android.jar}. The backend's base {@code AndroidApplication} is
     * rewritten to extend the loader's own {@link LoaderActivity}, which is
     * resolved from the loader jar classpath at compile time.
     */
    private static void buildAndroidComponent() {
        if (ArtPlatform.gameAndroidCompFile.exists())
            return;
        Log.debug("Building android components.");
        try (var zos = new ZipOutputStream(new FileOutputStream(ArtPlatform.gameAndroidCompFile)))  {
            JvmCompiler compiler = new JvmCompiler();
            compiler.addClassPath(readInternalFile("java-stub-rt.jar"));
            compiler.addClassPath(readInternalFile("android.jar"), getAndroidJarFilter());
            compiler.addClassPath(ArtPlatform.gameFile, getRawGameJarFilter());
            compiler.addClassPath(ArtPlatform.jarFile, getLoaderJarFilter());
            compiler.setVersion("17", "17");

            walkZip(arcSrc, f -> {
                String name = getSrcFileRealPath(f.getName());
                if (name.startsWith("backends/backend-android/src/") && name.endsWith(".java")) {
                    byte[] codeBytes = Streams.readAllBytes(arcSrc.getInputStream(f));
                    String code = new String(codeBytes, StandardCharsets.UTF_8);

                    // the easiest way to replace the parent class
                    if (name.endsWith("/AndroidApplication.java"))
                        code = code.replace(" class AndroidApplication extends Activity ", " class AndroidApplication extends copper.launch.LoaderActivity ");

                    compiler.addSource("backends/backend-android/src/", name, code);
                }
            });
            walkZip(gameSrc, f -> {
                String name = getSrcFileRealPath(f.getName());
                if (name.startsWith("android/src/") && name.endsWith(".java")) {
                    byte[] codeBytes = Streams.readAllBytes(gameSrc.getInputStream(f));
                    String code = new String(codeBytes, StandardCharsets.UTF_8);
                    compiler.addSource("android/src/", name, code);
                }
            });

            compiler.compile();
            if (compiler.hasErrors()) {
                StringBuilder b = new StringBuilder();
                b.append("failed to compile android components: ");
                for (String e : compiler.getErrors()) {
                    b.append('\n');
                    b.append(e);
                }
                throw new RuntimeException(b.toString());
            }

            for (var e : compiler.getBytecodes().entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey().replace('.', '/') + ".class");
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
            }
        } catch (Throwable e) {
            ArtPlatform.gameAndroidCompFile.delete();
            throw new RuntimeException("failed to build android components", e);
        }
    }

    /**
     * Copies every non-class file of the desktop game jar into {@code asset.jar}
     * under an {@code assets/} prefix, plus a dummy manifest to satisfy the
     * strict apk verification.
     */
    private static void packGameAssets() {
        if (ArtPlatform.gameAssetFile.exists())
            return;
        Log.debug("Packing game assets.");
        try (var zos = new ZipOutputStream(new FileOutputStream(ArtPlatform.gameAssetFile))) {
            walkZip(gameJar, f -> {
                if (f.isDirectory())
                    return;
                String name = f.getName().replace('\\', '/');
                if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib") || name.endsWith(".class"))
                    return;
                if (name.contains("META-INF"))
                    return;
                if (name.startsWith("/"))
                    name = name.substring(1);

                ZipEntry t = new ZipEntry("assets/" + name);
                zos.putNextEntry(t);
                Streams.pipeStream(gameJar.getInputStream(f), zos, true);
                zos.closeEntry();
            });

            // bypass strict apk verify
            ZipEntry t = new ZipEntry("AndroidManifest.xml");
            zos.putNextEntry(t);
            // i know AndroidManifest.xml is a compiled xml, but they don't check the content
            // just make xml not empty
            zos.write("<manifest package=\"io.anuken.mindustry.assets\" />".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (Throwable e) {
            ArtPlatform.gameAssetFile.delete();
            throw new RuntimeException("failed to pack game assets", e);
        }
    }

    /** Packs the android native libraries (.so files) from the arc source zip. */
    private static void packGameLibs() {
        if (ArtPlatform.gameLibFile.exists())
            return;
        Log.debug("Packing game libs.");
        try (var zos = new ZipOutputStream(new FileOutputStream(ArtPlatform.gameLibFile))) {
            walkZip(arcSrc, f -> {
                if (f.isDirectory())
                    return;

                String name = getSrcFileRealPath(f.getName());
                ZipEntry t = null;
                if (name.startsWith("natives/natives-android/libs/") && name.endsWith(".so"))
                    t = new ZipEntry(name.substring("natives/natives-android/libs/".length()));
                if (name.startsWith("natives/natives-freetype-android/libs/") && name.endsWith(".so"))
                    t = new ZipEntry(name.substring("natives/natives-freetype-android/libs/".length()));

                if (t != null) {
                    zos.putNextEntry(t);
                    Streams.pipeStream(arcSrc.getInputStream(f), zos, true);
                    zos.closeEntry();
                }
            });
        } catch (Throwable e) {
            ArtPlatform.gameLibFile.delete();
            throw new RuntimeException("failed to pack game libs", e);
        }
    }

    /**
     * Adds the java runtime stub and android.jar bytecode to the loader container,
     * so the mixin engine can resolve java/android classes while transforming.
     */
    private static void loadLoaderResource() {
        Log.verbose("Loading loader resource.");
        Loader.vars.loaderContainer.resource.resources.add(new BytecodeResource(readInternalFile("java-stub-rt.jar"), null));
        Loader.vars.loaderContainer.resource.resources.add(new BytecodeResource(readInternalFile("android.jar"), getAndroidJarFilter()));
    }

    /** Turns on mixin audit logging for the game and every mod. */
    private static void enableMixinLog() {
        Loader.game.container.setMixinLogEnabled(true);
        Loader.mods.eachMod(m -> m.container.setMixinLogEnabled(true));
    }

    /** Registers one resource entry per mod plus {@code loader} and {@code mindustry};
     *  the actual jars are only opened when first needed. */
    private static void loadResouceEntry() {
        Log.debug("Loading recource entries.");
        Loader.mods.eachMod(m -> {
            try {
                resourceMap.put(m.id, null);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        resourceMap.put("loader", null);
        resourceMap.put("mindustry", null);
    }

    /**
     * Loads the system libraries shared by every dex compile: android.jar,
     * the desugar libs, and the desugar config json.
     */
    private static void loadSystemResource() {
        Log.verbose("Loading system recource.");
        byte[] desugarConfigJarBytes = readInternalFile("desugar_jdk_libs_configuration.jar");

        // find the desugar.json inside the config jar
        try (var zis = new ZipInputStream(new ByteArrayInputStream(desugarConfigJarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.equals("META-INF/desugar/d8/desugar.json")) {
                    desugarConfig = new String(Streams.readAllBytes(zis), StandardCharsets.UTF_8);
                    zis.closeEntry();
                    break;
                }
                zis.closeEntry();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        systemResource.add(new D8Resource(readInternalFile("android.jar")));
        systemResource.add(new D8Resource(readInternalFile("desugar_jdk_libs.jar")));
        systemResource.add(new D8Resource(desugarConfigJarBytes));
    }

    /**
     * Dexes every mod once (without mixins) and stores the result as its base dex pool.
     * Later, mixin dexes are built by merging these pools with the mixin output.
     */
    private static void buildBaseDexPool() {
        Log.debug("Building base dex pools.");
        for (var id : resourceMap.keySet()) {
            if (id.equals("loader"))
                continue;

            Version version = id.equals("mindustry") ?
                    Loader.game.version : Loader.mods.getModById(id).version;
            File poolFile = dexCache.getBaseDexFile(id, version.toString());
            if (poolFile.exists())
                continue;
            Log.debug("Building base dex pool for: " + id);

            D8Resource resource = loadResource(id);
            DexCompiler compiler = buildDexCompiler(id, resource, false, null);
            compiler.compile();

            BaseDexPool pool = new BaseDexPool();
            for (var e : compiler.getBytecodes().entrySet())
                pool.putCode(e.getKey(), e.getValue());
            pool.write(poolFile);
            // save heap memory on android
            if (ArtBuilderPlatform.desktopMode)
                baseDexPoolMap.put(id, pool);
        }
    }

    /**
     * Applies the mixins targeting {@code id} and collects the transformed classes
     * (targets plus their transitive dependencies) into a delta source filter.
     * Computed once per mod and cached in {@link #deltaSourceFilter}.
     */
    private static void applyMixin(String id) {
        if (deltaSourceFilter.containsKey(id))
            return;
        if (id.equals("loader"))
            return;

        MixinContainer container = id.equals("mindustry") ?
                Loader.game.container : Loader.mods.getModById(id).container;
        if (container.mixins.isEmpty()) {
            deltaSourceFilter.put(id, null);
            return;
        }

        Log.verbose("Applying mixins for: " + id);

        // the mixin engine needs the full loader environment, boot it once
        if (!loaderLaunched) {
            loadLoaderResource();
            Loader.launch();
            loaderLaunched = true;
        }

        // read the @Mixin targets out of every mixin class
        Set<String> target = new HashSet<>();
        for (var info : container.mixins) {
            for (var name : info.mixinName) {
                byte[] code = info.container.getOwnBytecode(info.packageName + "." + name);
                if (code == null)
                    continue;
                ClassReader cr = new ClassReader(code);
                MixinTargetExtractor extractor = new MixinTargetExtractor();
                cr.accept(extractor, 0);
                target.addAll(extractor.getTargets());
            }
        }
        if (Log.getLevel() == Log.Level.VERBOSE) {
            for (String name : target)
                Log.verbose("Found mixin target class: " + name);
        }

        // take the transformed bytecode of each target and everything it
        // depends on, recursively; that set becomes the mixin "delta"
        D8Resource resource = loadResource(id);
        ClassFilter filter = new ClassFilter();
        Cons2<String, Set<String>> process = (name, dependency) -> {
            Log.verbose("Processing mixin class: " + name);
            byte[] code = container.getOwnBytecode(name);
            resource.putCode(name, code);
            filter.addRule("include " + name);

            ClassReader cr = new ClassReader(code);
            DependencyExtractor extractor = new DependencyExtractor();
            cr.accept(extractor, 0);
            for (String dep : extractor.getDependencies()) {
                if (resource.hasCode(dep) || container.getOwnBytecode(dep) == null)
                    continue;
                dependency.add(dep);
                Log.verbose("Found mixin generated class: " + dep);
            }
        };

        Set<String> dependency = new HashSet<>();
        for (String name : target) {
            // skip not found pseudo mixin
            if (!resource.hasCode(name))
                continue;
            process.get(name, dependency);
        }
        // keep expanding until no new dependency appears
        Set<String> toProcess = new HashSet<>();
        while (!dependency.isEmpty()) {
            for (String name : dependency)
                process.get(name, toProcess);
            var tmp = dependency;
            dependency = toProcess;
            toProcess = tmp;
            toProcess.clear();
        }
        deltaSourceFilter.put(id, filter);
    }

    /**
     * Dexes each mod again with its mixin delta as extra source and merges the
     * result with the mod's base dex pool into a cached mixin dex jar, writing
     * its {@link MixinDexMeta} next to it.
     */
    private static void buildMixinDex() {
        Log.debug("Building mixin dex.");
        for (var id : resourceMap.keySet()) {
            if (id.equals("loader"))
                continue;

            MixinDexMeta meta = new MixinDexMeta();
            MixinContainer container;
            meta.id = id;
            if (id.equals("mindustry")) {
                container = Loader.game.container;
                meta.version = Loader.game.version.toString();
            } else {
                var mod = Loader.mods.getModById(id);
                container = mod.container;
                meta.version = mod.version.toString();
            }
            for (var info : container.mixins) {
                var ver = Loader.mods.getModById(info.container.id).version;
                meta.sources.add(new ModDescriptor(info.container.id, ver.toString()));
            }
            mixinDexMetaMap.put(id, meta);
        }

        // dex each mod again, this time including the mixin delta as source
        Map<String, Map<String, byte[]>> dexCode = new HashMap<>();
        for (var entry : mixinDexMetaMap.entrySet()) {
            var id = entry.getKey();
            var meta = entry.getValue();

            File dexFile = dexCache.getMixinDexFile(meta);
            if (dexFile.exists())
                continue;

            applyMixin(id);
            var filter = deltaSourceFilter.get(id);
            if (filter == null) {
                dexCode.put(id, null);
                continue;
            }

            Log.debug("Building dex for: " + id);
            D8Resource resource = loadResource(id);
            DexCompiler compiler = buildDexCompiler(id, resource, true, filter);
            compiler.compile();
            dexCode.put(id, compiler.getBytecodes());
        }

        // no needed anymore
        resourceMap.clear();
        if (systemResource != null)
            systemResource.clear();

        for (var entry : dexCode.entrySet()) {
            String id = entry.getKey();
            var code = entry.getValue();
            var meta = mixinDexMetaMap.get(id);
            File metaFile = dexCache.getMixinDexMetaFile(meta);
            File dexFile = dexCache.getMixinDexFile(meta);

            try {
                Log.debug("Merging dex for: " + id);
                MixinDex dex = new MixinDex(loadBaseDexPool(id));
                if (code != null) {
                    for (var e : code.entrySet())
                        dex.putCode(e.getKey(), e.getValue());
                }
                dex.build(dexFile);
                meta.write(metaFile);
            } catch (Throwable e) {
                dexFile.delete();
                metaFile.delete();
                throw e;
            }
        }
    }

    /**
     * Writes the runtime meta for the current mod set: every loaded mod paired
     * with the hash of the mixin dex jar it must load.
     */
    private static void buildRuntime() {
        Log.debug("Building runtime.");
        RuntimeMeta meta = new RuntimeMeta();
        Loader.mods.eachMod(m -> meta.mods.add(new ModDescriptor(m.id, m.version.toString())));
        for (var entry : mixinDexMetaMap.entrySet())
            meta.jarLinks.put(entry.getKey(), entry.getValue().sha256());
        dexCache.updateCurrentRuntime(meta);
    }

    // lazy load resource
    /**
     * Opens the jar of {@code id} on first use and caches the opened resource.
     * The mindustry resource also includes the compiled android components.
     */
    private static D8Resource loadResource(String id) {
        D8Resource resource = resourceMap.get(id);
        if (resource != null)
            return resource;
        if (!resourceMap.containsKey(id))
            throw new RuntimeException("missing resource entry: " + id);
        try {
            Log.verbose("Loading resource: " + id);
            if (id.equals("mindustry")) {
                resource = new D8Resource(ArtPlatform.gameFile, getRawGameJarFilter());
                resource.putAllCode(new D8Resource(ArtPlatform.gameAndroidCompFile));
            } else if (id.equals("loader")) {
                resource = new D8Resource(ArtPlatform.jarFile, getLoaderJarFilter());
            } else {
                resource = new D8Resource(Loader.mods.getModById(id).file);
            }
            resourceMap.put(id, resource);
            return resource;
        } catch (Throwable e) {
            throw new RuntimeException("failed to load resource: " + id);
        }
    }

    /** Loads a base dex pool, either from memory (desktop) or from the cache file. */
    private static BaseDexPool loadBaseDexPool(String id) {
        if (ArtBuilderPlatform.desktopMode && baseDexPoolMap.containsKey(id)) {
            return baseDexPoolMap.get(id);
        } else {
            Version version = id.equals("mindustry") ?
                    Loader.game.version : Loader.mods.getModById(id).version;
            File poolFile = dexCache.getBaseDexFile(id, version.toString());
            if (!poolFile.exists())
                throw new RuntimeException("base dex pool not found: " + id + " : " + version);
            Log.verbose("Loading base dex pool: " + id + " : " + version);
            return new BaseDexPool(poolFile);
        }
    }

    // only use in jvm compiler
    /**
     * Drops the {@code java.*}/{@code javax.*} classes of android.jar (they are
     * provided by the java-stub-rt.jar), keeping the android classes plus the
     * {@code javax.microedition} package.
     */
    private static ClassFilter getAndroidJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("include javax.microedition.*");
        filter.addRule("exclude java.*");
        filter.addRule("exclude javax.*");
        filter.addRule("include *");
        return filter;
    }

    /** Drops the desktop-only classes from the game jar. */
    private static ClassFilter getRawGameJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("exclude arc.backend.sdl.*");
        filter.addRule("exclude mindustry.desktop.*");
        filter.addRule("exclude steamworks.*");
        filter.addRule("include *");
        return filter;
    }

    /** Drops the build-time only classes from the loader jar (r8, jdt, ...). */
    private static ClassFilter getLoaderJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("exclude com.android.tools.*");
        filter.addRule("exclude com.google.*");
        filter.addRule("exclude org.eclipse.jdt.*");
        filter.addRule("include *");
        return filter;
    }

    /**
     * Creates a d8 {@link DexCompiler} for one mod.
     *
     * @param id          the mod (or {@code "mindustry"}) being compiled
     * @param resource    its class resource
     * @param withMixin   whether to add the mod's mixin containers as libraries
     * @param deltaSource extra source filter selecting the mixin-transformed classes
     */
    private static DexCompiler buildDexCompiler(String id, D8Resource resource, boolean withMixin, ClassFilter deltaSource) {
        DexCompiler compiler = new DexCompiler();
        compiler.setId(id);
        compiler.addClassPath(resource);

        if (systemResource.isEmpty())
            loadSystemResource();

        if (desugarConfig != null)
            compiler.addDesugaredLibraryConfig(desugarConfig);
        for (var res : systemResource)
            compiler.addLibrary(res);

        MixinContainer container = id.equals("mindustry") ?
                Loader.game.container : Loader.mods.getModById(id).container;

        // access mixins (interfaces with @Accessor/@Invoker) are needed by the
        // containers that use them, so keep them as source; drop the rest of the
        // consumed mixin packages
        Set<String> processedPackage = new HashSet<>();
        ClassFilter srcFilter = new ClassFilter();
        if (!id.equals("mindustry")) {
            for (var desc : Loader.mods.getModById(id).mixins) {
                MixinContainer target = desc.id.equals("mindustry") ?
                        Loader.game.container : Loader.mods.getModById(desc.id).container;
                for (var info : target.mixins) {
                    if (info.container == container && processedPackage.add(info.packageName)) {
                        String prefix = info.packageName + ".";
                        resource.eachCode((name, code) -> {
                            if (name.startsWith(prefix)) {
                                ClassReader cr = new ClassReader(code);
                                if ((cr.getAccess() & Opcodes.ACC_INTERFACE) == 0)
                                    return;
                                AccessMixinChecker checker = new AccessMixinChecker();
                                cr.accept(checker, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                                if (checker.isAccessMixin() || !checker.isMixin()) {
                                    srcFilter.addRule("include " + name);
                                }
                            }
                        });
                        srcFilter.addRule("exclude " + info.packageName + ".*");
                    }
                }
            }
        }
        srcFilter.addRule("include *");
        var src = resource.getFiltered(srcFilter);
        if (deltaSource != null)
            src = src.getFiltered(deltaSource);
        compiler.addSource(src);

        // adding the mixin containers as libraries lets d8 link against their classes
        if (withMixin) {
            for (var mixin : container.mixins) {
                applyMixin(mixin.container.id);
                var lib = loadResource(mixin.container.id)
                        .getFiltered(mixin.container.export);
                compiler.addLibrary(lib);
            }
        }

        // every dependency's exported classes must be visible while compiling
        for (var dep : container.dependencies) {
            var filter = dep.extraImport.copy();
            filter.addAllRules(dep.container.export);
            if (withMixin)
                applyMixin(dep.container.id);
            var lib = loadResource(dep.container.id)
                    .getFiltered(filter);
            compiler.addLibrary(lib);
        }

        return compiler;
    }

    private static void checkFileProvided(File file, String desc) {
        if (file == null)
            throw new RuntimeException(desc + " is not provided");
    }

    private static void checkFileExists(File file, String desc) {
        checkFileProvided(file, desc);
        if (!file.exists())
            throw new RuntimeException(desc + " is not existed: " + file.getAbsolutePath());
    }

    /** Iterates over every entry of a zip file. */
    private static void walkZip(ZipFile file, ThrowableCons<ZipEntry> cons) {
        var e = file.entries();
        try {
            while (e.hasMoreElements())
                cons.get(e.nextElement());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** Reads a file embedded in the loader jar. */
    private static byte[] readInternalFile(String path) {
        try {
            return Streams.readAllBytes(loaderJar.getInputStream(loaderJar.getEntry(path)));
        } catch (Throwable e) {
            throw new RuntimeException("failed to read internal file: " + path, e);
        }
    }

    /** Strips the leading module/source-root segment from a source zip path. */
    private static String getSrcFileRealPath(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/"))
            path = path.substring(1);
        return path.substring(path.indexOf('/') + 1);
    }
}