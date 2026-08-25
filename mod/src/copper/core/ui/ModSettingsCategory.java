package copper.core.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import copper.core.*;
import copper.core.patch.*;
import copper.core.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

@Internal
public class ModSettingsCategory extends SettingsMenuDialog.SettingsCategory {
    private String searchText;
    private Table settingsTable;
    private TextField field;
    private OrderedMap<Mods.LoadedMod, Seq<SettingsMenuDialog.SettingsCategory>> modMap;
    private Seq<SettingsMenuDialog.SettingsCategory> unknownModList;

    public ModSettingsCategory() {
        super(CoreMod.bundles.get("settings.mod-settings"), Icon.book, t -> {});
        searchText = "";
        modMap = new OrderedMap<>();
        unknownModList = new Seq<>();
        table = new SettingsTable();
        build();
    }

    public void show() {
        modMap.clear();
        for (var cat : Vars.ui.settings.getCategories()) {
            if (cat instanceof ModSettingsCategory)
                continue;
            if (cat instanceof ICopperSettingsCategory ccat && ccat.getLoadedMod() != null) {
                var list = modMap.get(ccat.getLoadedMod());
                if (list == null) {
                    list = new Seq<>();
                    modMap.put(ccat.getLoadedMod(), list);
                }
                list.add(cat);
            } else {
                unknownModList.add(cat);
            }
        }

        modMap.orderedKeys().sort(Structs.comparing(m -> m.meta.displayName == null ? m.meta.name : m.meta.displayName)).reverse();
        unknownModList.sort(Structs.comparing(c -> c.name)).reverse();

        field.setText(searchText = "");
        rebuild();
        Core.app.post(field::requestKeyboard);
    }

    private void build() {
        table.top();

        table.table(t -> {
            t.left();
            t.image(Icon.zoom);
            field = t.field(searchText, res -> {
                searchText = res.trim();
                rebuild();
            }).growX().get();
            field.setMessageText(CoreMod.bundles.get("settings.mod-settings.search-mod"));
        }).width(300f).padBottom(8).top();
        table.row();

        settingsTable = table.table()
                .get();
    }

    private void rebuild() {
        settingsTable.clearChildren();

        boolean empty = true;
        for (var entry : modMap) {
            String modName = entry.key.meta.displayName == null ?
                    entry.key.meta.name : entry.key.meta.displayName;
            if (!searchText.isEmpty() && !modName.toLowerCase().contains(searchText.toLowerCase()))
                continue;
            empty = false;
            addModSettings(modName, entry.value);
        }

        String unknownMod = CoreMod.bundles.get("settings.mod-settings.unknown-mod");
        if (searchText.isEmpty() || unknownMod.toLowerCase().contains(searchText.toLowerCase())) {
            if (!unknownModList.isEmpty()) {
                empty = false;
                addModSettings(unknownMod, unknownModList);
            }
        }

        if (empty) {
            settingsTable.add(CoreMod.bundles.get(searchText.isEmpty() ? "empty" : "not-found"))
                    .color(Color.gray).colspan(4).pad(10).padTop(12).row();
        }
    }

    private void addModSettings(String name, Seq<SettingsMenuDialog.SettingsCategory> list) {
        float width = 300f;
        float height = 60f;
        float marg = 8f;

        settingsTable.add(name)
                .color(Color.gray).colspan(4).pad(10).padBottom(4).row();
        settingsTable.image()
                .color(Color.gray).width(width).height(3).pad(6).colspan(4).padTop(0).padBottom(10).row();

        for (var cat : list) {
            if (cat.icon == null) {
                settingsTable.button(cat.name, Styles.grayt, () -> (new ModSettingsDialog(cat)).show())
                        .marginLeft(marg).width(width).height(height);
            } else {
                settingsTable.button(cat.name, cat.icon, Styles.grayt, Vars.iconMed, () -> (new ModSettingsDialog(cat)).show())
                        .with(b -> ((Image) b.getChildren().get(1)).setScaling(Scaling.fit))
                        .marginLeft(marg).width(width).height(height);
            }
            settingsTable.row();
        }
    }

    public static class SettingsTable extends SettingsMenuDialog.SettingsTable {}

    private static class ModSettingsDialog extends BaseDialog {
        public ModSettingsDialog(SettingsMenuDialog.SettingsCategory cat) {
            super(Strings.stripColors(cat.name.startsWith("@") ?
                    Core.bundle.get(cat.name.substring(1)) : cat.name));
            addCloseButton();
            Table t = new Table();
            t.add(cat.table);
            cont.pane(t)
                    .grow().top();
        }
    }
}
