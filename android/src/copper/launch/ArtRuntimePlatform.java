package copper.launch;

import android.os.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mixin.*;
import copper.loader.util.*;

import java.io.*;
import java.util.zip.*;

/**
 * The device-side {@link ArtPlatform}: loads the prebuilt dex cache at runtime.
 *
 * <p>Mixins were already applied when the cache was built, so the runtime only
 * needs a {@link MockMixinEngine}. Containers load their classes from the built
 * dex files via {@link ArtPreMixinContainer}.</p>
 */
public class ArtRuntimePlatform extends ArtPlatform {
    /** Cache of the built dex files, shared with the launcher. */
    public static DexCache dexCache;

    /** Mixins are pre-applied, so the engine does nothing at runtime. */
    @Override
    public IMixinEngine createMixinEngine() {
        return new MockMixinEngine();
    }

    @Override
    protected MixinContainer createJarContainer(File file) {
        try {
            MixinContainer container = new ArtPreMixinContainer();
            if (file != null)
                container.resource.resources.add(new ZipResource(new ZipFile(file)));
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create container for: " + file.getAbsolutePath(), e);
        }
    }

    /** Returns the ABI of the current device. */
    @Override
    protected String getAbi() {
        return Build.SUPPORTED_ABIS[0];
    }
}