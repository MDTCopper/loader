package copper.launch;

import android.app.*;
import android.content.*;
import android.content.res.loader.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import copper.loader.Loader;
import copper.loader.util.*;
import java.io.*;

public class LoaderActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> finishAndRemoveTask());
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(ArtPlatform.gameAssetFile, ParcelFileDescriptor.MODE_READ_ONLY);
            ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
            ResourcesLoader loader = new ResourcesLoader();
            loader.addProvider(provider);
            getResources().addLoaders(loader);
        } catch (Throwable e) {
            throw new RuntimeException("failed to inject resource", e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            String title = "Mindustry (Copper)";
            byte[] iconData;
            try (var fis = getAssets().open("icons/icon_64.png")) {
                iconData = Streams.readAllBytes(fis);
            }
            Bitmap icon = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
            var description = new ActivityManager.TaskDescription(title, icon);
            setTaskDescription(description);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getFilesDir() {
        return Loader.vars.gameDataFolder;
    }

    @Override
    public File getExternalFilesDir(String type) {
        return Loader.vars.gameDataFolder;
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return new File[]{Loader.vars.gameDataFolder};
    }

    @Override
    public File getCacheDir() {
        File cache = new File(super.getCacheDir(), "mindustry");
        cache.mkdirs();
        return cache;
    }


}
