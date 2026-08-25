package copper.core.patch;

import mindustry.mod.*;

public interface ICopperSettingsCategory {
    Mods.LoadedMod getCopperDetectedLoadedMod();
    String getCopperDetectedModName();
    void setCopperDetectedModName(String modName);
}
