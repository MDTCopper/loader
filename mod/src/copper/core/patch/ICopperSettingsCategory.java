package copper.core.patch;

import mindustry.mod.*;

public interface ICopperSettingsCategory {
    Mods.LoadedMod getLoadedMod();
    String getModName();
    void setModName(String modName);
}
