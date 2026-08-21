package copper.launch;

import copper.launch.asm.*;
import copper.launch.builder.*;
import copper.launch.util.*;
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

public class ArtBuilder {
    private static DexCache dexCache;
    private static ZipFile gameJar;
    private static ZipFile loaderJar;
    private static ZipFile gameSrc;
    private static ZipFile arcSrc;

    private static Map<String, D8Resource> resourceMap;
    private static List<D8Resource> systemResource;
    private static Map<String, BaseDexPool> baseDexPoolMap;
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
                loadLoaderResource();
                Loader.init();
                dexCache.init();
                if (!dexCache.isCurrentRuntimeExisted()) {
                    if (parser.hasOption("verbose"))
                        enableMixinLog();
                    loadResouce();
                    loadSystemResource();
                    buildBaseDexPool();
                    Loader.launch();
                    buildRuntimeDex();
                }
            }

            Log.info("Done.");
        } catch (Throwable e) {
            Log.error(e);
            throw new RuntimeException("failed to execute builder command", e);
        }
    }

    private static void buildAndroidComponent() {
        if (ArtPlatform.gameAndroidCompFile.exists())
            return;
        Log.verbose("Building android components.");
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

    private static void packGameAssets() {
        if (ArtPlatform.gameAssetFile.exists())
            return;
        Log.verbose("Packing game assets.");
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

    private static void packGameLibs() {
        if (ArtPlatform.gameLibFile.exists())
            return;
        Log.verbose("Packing game libs.");
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

    private static void loadLoaderResource() {
        Log.verbose("Loading loader resource.");
        Loader.vars.loaderContainer.resource.resources.add(new BytecodeResource(readInternalFile("java-stub-rt.jar"), null));
        Loader.vars.loaderContainer.resource.resources.add(new BytecodeResource(readInternalFile("android.jar"), getAndroidJarFilter()));
    }

    private static void enableMixinLog() {
        Loader.game.container.setMixinLogEnabled(true);
        Loader.mods.eachMod(m -> m.container.setMixinLogEnabled(true));
    }

    private static void loadResouce() {
        Log.verbose("Loading recource.");
        resourceMap = new HashMap<>();
        try {
            D8Resource mdt = new D8Resource(ArtPlatform.gameFile, getRawGameJarFilter());
            mdt.putAllCode(new D8Resource(ArtPlatform.gameAndroidCompFile));
            resourceMap.put("mindustry", mdt);
            resourceMap.put("loader", new D8Resource(ArtPlatform.jarFile, getLoaderJarFilter()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Loader.mods.eachMod(m -> {
            try {
                resourceMap.put(m.id, new D8Resource(m.file));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void loadSystemResource() {
        Log.verbose("Loading system recource.");
        systemResource = new ArrayList<>();
        byte[] desugarConfigJarBytes = readInternalFile("desugar_jdk_libs_configuration.jar");

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

    private static void buildBaseDexPool() {
        Log.verbose("Building base dex pools.");
        baseDexPoolMap = new HashMap<>();
        for (var entry : resourceMap.entrySet()) {
            String id = entry.getKey();
            D8Resource resource = entry.getValue();
            if (id.equals("loader"))
                continue;

            Version version = id.equals("mindustry") ?
                    Loader.game.version : Loader.mods.getModById(id).version;
            File poolFile = dexCache.getBaseDexFile(id, version.toString());
            if (poolFile.exists())
                continue;
            Log.verbose("  -> " + id + " : " + version);

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

    private static void buildRuntimeDex() throws Throwable {
        try {
            Log.verbose("Building runtime dex jars.");
            Map<String, ClassFilter> sourceFilter = new HashMap<>();
            for (var entry : resourceMap.entrySet()) {
                String id = entry.getKey();
                D8Resource resource = entry.getValue();
                if (id.equals("loader"))
                    continue;

                Log.verbose("Applying mixins for: " + id);

                MixinContainer container = id.equals("mindustry") ?
                        Loader.game.container : Loader.mods.getModById(id).container;
                if (container.mixin.isEmpty()) {
                    sourceFilter.put(id, null);
                    continue;
                }

                Set<String> target = new HashSet<>();
                for (var info : container.mixin) {
                    for (var name : info.mixinName) {
                        byte[] code = info.container.getOwnBytecode(info.packageName + "." + name);
                        if (code == null)
                            continue;
                        ClassReader cr = new ClassReader(code);
                        MixinTargetExtractor extractor = new MixinTargetExtractor();
                        cr.accept(extractor, 0);
                        target.addAll(extractor.getTarget());
                    }
                }
                if (Log.getLevel() == Log.Level.VERBOSE) {
                    for (String name : target)
                        Log.verbose("Found mixin target class: " + name);
                }

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
                Set<String> toProcess = new HashSet<>();
                while (!dependency.isEmpty()) {
                    for (String name : dependency)
                        process.get(name, toProcess);
                    var tmp = dependency;
                    dependency = toProcess;
                    toProcess = tmp;
                    toProcess.clear();
                }
                sourceFilter.put(id, filter);
            }

            Map<String, Map<String, byte[]>> dexCode = new HashMap<>();
            for (var entry : resourceMap.entrySet()) {
                String id = entry.getKey();
                D8Resource resource = entry.getValue();
                if (id.equals("loader"))
                    continue;

                ClassFilter filter = sourceFilter.get(id);
                if (filter == null) {
                    dexCode.put(id, null);
                    continue;
                }

                Log.verbose("Building dex for: " + id);
                DexCompiler compiler = buildDexCompiler(id, resource, true, filter);
                compiler.compile();
                dexCode.put(id, compiler.getBytecodes());
            }

            // no needed anymore
            resourceMap.clear();
            systemResource.clear();

            for (var entry : dexCode.entrySet()) {
                String id = entry.getKey();
                var code = entry.getValue();
                File dexFile;

                boolean buildLink = code == null ||
                        (id.equals("mindustry") && Loader.game.container.mixin.size() == 1 &&
                                Loader.game.container.mixin.get(0).container.id.equals("copper:core"));

                if (buildLink) {
                    Version version = id.equals("mindustry") ?
                            Loader.game.version : Loader.mods.getModById(id).version;
                    dexFile = dexCache.getPackedBaseDexFile(id, version.toString());
                } else {
                    dexFile = dexCache.getRuntimeDexFile(id);
                }

                if (!dexFile.exists()) {
                    Log.verbose("Merging dex for: " + id);
                    RuntimeDex dex = new RuntimeDex(loadBaseDexPool(id));
                    if (code != null) {
                        for (var e : code.entrySet())
                            dex.putCode(e.getKey(), e.getValue());
                    }
                    dex.build(dexFile);
                }

                if (buildLink) {
                    File link = dexCache.getRuntimeDexLink(id);
                    try (var fos = new FileOutputStream(link)) {
                        fos.write(dexFile.getName().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Throwable e) {
            dexCache.clearCurrentRuntime();
            throw e;
        }
    }

    private static BaseDexPool loadBaseDexPool(String id) {
        if (ArtBuilderPlatform.desktopMode) {
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
    private static ClassFilter getAndroidJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("include javax.microedition.*");
        filter.addRule("exclude java.*");
        filter.addRule("exclude javax.*");
        filter.addRule("include *");
        return filter;
    }

    private static ClassFilter getRawGameJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("exclude arc.backend.sdl.*");
        filter.addRule("exclude mindustry.desktop.*");
        filter.addRule("exclude steamworks.*");
        filter.addRule("include *");
        return filter;
    }

    private static ClassFilter getLoaderJarFilter() {
        ClassFilter filter = new ClassFilter();
        filter.addRule("exclude com.android.tools.*");
        filter.addRule("exclude com.google.*");
        filter.addRule("exclude org.eclipse.jdt.*");
        filter.addRule("include *");
        return filter;
    }

    private static DexCompiler buildDexCompiler(String id, D8Resource resource, boolean withMixin, ClassFilter deltaSource) {
        DexCompiler compiler = new DexCompiler();
        compiler.addClassPath(resource);

        if (desugarConfig != null)
            compiler.addDesugaredLibraryConfig(desugarConfig);
        for (var res : systemResource)
            compiler.addLibrary(res);

        MixinContainer container = id.equals("mindustry") ?
                Loader.game.container : Loader.mods.getModById(id).container;

        Set<String> processedPackage = new HashSet<>();
        ClassFilter srcFilter = new ClassFilter();
        if (!id.equals("mindustry")) {
            for (var desc : Loader.mods.getModById(id).mixin) {
                MixinContainer target = desc.id.equals("mindustry") ?
                        Loader.game.container : Loader.mods.getModById(desc.id).container;
                for (var info : target.mixin) {
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

        if (withMixin) {
            for (var mixin : container.mixin) {
                var lib = resourceMap.get(mixin.container.id)
                        .getFiltered(mixin.container.export);
                compiler.addLibrary(lib);
            }
        }

        for (var dep : container.dependency) {
            var filter = dep.extraImport.copy();
            filter.addAllRules(dep.container.export);
            var lib = resourceMap.get(dep.container.id)
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

    private static void walkZip(ZipFile file, ThrowableCons<ZipEntry> cons) {
        var e = file.entries();
        try {
            while (e.hasMoreElements())
                cons.get(e.nextElement());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static byte[] readInternalFile(String path) {
        try {
            return Streams.readAllBytes(loaderJar.getInputStream(loaderJar.getEntry(path)));
        } catch (Throwable e) {
            throw new RuntimeException("failed to read internal file: " + path, e);
        }
    }

    private static String getSrcFileRealPath(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/"))
            path = path.substring(1);
        return path.substring(path.indexOf('/') + 1);
    }
}
