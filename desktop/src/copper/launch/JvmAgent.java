package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

public class JvmAgent {
    private static Instrumentation instrumentation;
    private static Mod debugMod;

    public static void agentmain(String arg, Instrumentation instrumentation) {
        JvmAgent.instrumentation = instrumentation;
    }

    /**
     * Puts the classpath of a debugged mod in front of its jar and asks Mixin for hot swap.
     *
     * <p>Runs before the game starts: the injected classes must win before the mod is loaded,
     * and the mixin transformer decides whether it gets a hot swap agent while it is created,
     * which only reads the option from the system properties.</p>
     */
    public static void attachDebug(Mod mod, File classpath) {
        if (instrumentation == null) {
            Log.warn("the launcher agent is not running, ignoring mod debug request");
            return;
        }
        try {
            IResource resource = classpath.isDirectory() ?
                    new FolderResource(classpath) : new ZipResource(new ZipFile(classpath));
            mod.container.resource.resources.add(0, resource);
        } catch (Throwable e) {
            throw new RuntimeException("failed to inject classpath into the mod to debug", e);
        }

        System.setProperty("mixin.hotSwap", "true");
        debugMod = mod;
    }

    /**
     * Hands the instrumentation to the mixin agent of every container that can be redefined.
     *
     * <p>Runs after {@code Loader.launch()}: containers build their mixin engine there, and
     * there is nothing to set up on a container before that.</p>
     */
    public static void launchDebug() {
        if (instrumentation == null || debugMod == null)
            return;
        Mod mod = debugMod;
        debugMod = null;

        Set<String> processed = new HashSet<>();
        processed.add(mod.id);
        ((JvmMixinContainer) mod.container).setupAgent(instrumentation, true, true);
        for (var mixin : mod.mixins) {
            if (!processed.add(mixin.id))
                continue;
            var c = findContainer(mixin.id);
            c.setupAgent(instrumentation, false, true);
        }

        if (!processed.contains("mindustry")) {
            if (!Loader.game.container.mixins.isEmpty())
                ((JvmMixinContainer) Loader.game.container).setupAgent(instrumentation, false, false);
        }
        Loader.mods.eachMod(m -> {
            if (processed.contains(m.id))
                return;
            if (!m.container.mixins.isEmpty())
                ((JvmMixinContainer) m.container).setupAgent(instrumentation, false, false);
        });

        instrumentation.addTransformer(new RedefineFilter());
    }

    public static void dispose() {
        instrumentation = null;
        debugMod = null;
    }

    public static boolean isAttached() {
        return instrumentation != null && debugMod != null;
    }

    private static JvmMixinContainer findContainer(String id) {
        if (id.equals("mindustry"))
            return (JvmMixinContainer) Loader.game.container;
        Mod mod = Loader.mods.getModById(id);
        if (mod == null)
            throw new RuntimeException("mod not found: " + id);
        return (JvmMixinContainer) mod.container;
    }

    private static class RedefineFilter implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            MixinContainer container = null;
            if (loader == Loader.game.container.getClassLoader()) {
                container = Loader.game.container;
            } else {
                var mod = Loader.mods.getModByClassLoader(loader);
                if (mod != null)
                    container = mod.container;
            }

            if (container != null) {
                return !container.mixins.isEmpty() ? classfileBuffer :
                        container.getOwnBytecode(className.replace('/', '.'));
            }

            if (loader != JvmLauncher.class.getClassLoader())
                return classfileBuffer;
            // classes the JVM generates itself, such as proxies, have no class file to re-read
            InputStream stream = JvmLauncher.class.getClassLoader().getResourceAsStream(className + ".class");
            return stream == null ? classfileBuffer : Streams.readAllBytes(stream);
        }
    }
}
