package copper.core.patch.impl;

import arc.scene.ui.layout.*;
import arc.struct.*;
import copper.core.ui.*;
import mindustry.ui.dialogs.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Mixin(SettingsMenuDialog.class)
public class CSettingsMenuDialog {
    @Shadow
    private Seq<SettingsMenuDialog.SettingsCategory> categories;
    @Unique
    private boolean rebuildingMenu = false;
    @Unique
    private ModSettingsCategory category;

    @Inject(method = "rebuildMenu", at = @At("HEAD"))
    private void cSortCategories(CallbackInfo ci) {
        if (category == null)
            category = new ModSettingsCategory();

        if (categories.isEmpty())
            categories.add(category);
        int i = categories.indexOf(c -> c instanceof ModSettingsCategory);
        if (i == -1)
            categories.insert(0, category);
        else if (i > 1)
            categories.swap(0, i);
        rebuildingMenu = true;
    }

    @Redirect(method = "rebuildMenu", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"))
    private boolean cLimitBuiltCategory(Iterator<?> iterator) {
        if (rebuildingMenu) {
            rebuildingMenu = false;
            return true;
        }
        return false;
    }

    @Redirect(method = "visible", at = @At(value = "INVOKE", target = "Larc/struct/Seq;get(I)Ljava/lang/Object;"))
    private Object cToggleModSettings(Seq<Table> list, int index) {
        if (list.get(index) instanceof ModSettingsCategory.SettingsTable)
            category.show();
        return list.get(index);
    }
}
