package copper.core.util;

import arc.*;
import arc.util.*;

public class CopperSettings extends Settings {
    @Override
    public synchronized void load(){
        try{
            loadValues();
            // v146 has `keybinds.load();` here.
        }catch(Throwable error){
            Log.err("Error loading settings", error);
            if(errorHandler != null){
                if(!hasErrored) errorHandler.get(error);
            }else{
                throw error;
            }
            hasErrored = true;
        }
        //if loading failed, it still counts
        loaded = true;
    }
}
