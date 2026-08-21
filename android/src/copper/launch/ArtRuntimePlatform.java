package copper.launch;

import android.os.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mixin.*;
import copper.loader.util.*;

import java.io.*;
import java.util.zip.*;

public class ArtRuntimePlatform extends ArtPlatform {
    public static DexCache dexCache;

    @Override
    public IMixinEngine createMixinEngine() {
        return new MockMixinEngine();
    }

    @Override
    protected MixinContainer createJarContainer(File file) {
        try {
            MixinContainer container = new ArtPreMixinContainer();
            container.resource.resources.add(new ZipResource(new ZipFile(file)));
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create container for: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    protected String getAbi() {
        return Build.SUPPORTED_ABIS[0];
    }
}
