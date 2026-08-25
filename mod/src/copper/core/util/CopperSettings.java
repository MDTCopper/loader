package copper.core.util;

import arc.*;
import arc.util.*;

/**
 * A {@link Settings} subclass for Copper mods.
 *
 * <p>Overrides {@link #load()} to skip the {@code keybinds.load()} call
 * that was briefly present in Mindustry v146's {@link Settings#load()}
 * (and later removed). Copper mods use their own settings file and must not
 * trigger a reload of the game's global keybind data.</p>
 */
public class CopperSettings extends Settings {
    @Override
    public synchronized void load(){
        try{
            loadValues();
            // v146 has `keybinds.load();` here, which is removed in future version
            // and we don't want our setting instance to reload the game keybind info
        }catch(Throwable error){
            Log.err("Error loading settings", error);
            if(errorHandler != null){
                if(!hasErrored) errorHandler.get(error);
            }else{
                throw error;
            }
            hasErrored = true;
        }
        loaded = true;
        modified = false;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        modified = true;
        autosave();
    }

    @Override
    public synchronized void put(String name, Object object) {
        super.put(name, object);
        autosave();
    }

    @Override
    public synchronized void remove(String name) {
        super.remove(name);
        autosave();
    }
}
