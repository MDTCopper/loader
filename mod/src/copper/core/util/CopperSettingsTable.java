package copper.core.util;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class CopperSettingsTable extends Table {
    protected Seq<Setting> list;
    protected Settings settings;
    protected I18NBundle bundle;

    public CopperSettingsTable(Settings settings, I18NBundle bundle){
        left();
        list = new Seq<>();
        this.settings = settings;
        this.bundle = bundle;
    }

    public Seq<Setting> getSettings(){
        return list;
    }

    public void pref(Setting setting){
        list.add(setting);
        rebuild();
    }

    public SliderSetting sliderPref(String name, int def, int min, int max, SettingsMenuDialog.StringProcessor s){
        return sliderPref(name, def, min, max, 1, s);
    }

    public SliderSetting sliderPref(String name, int def, int min, int max, int step, SettingsMenuDialog.StringProcessor s){
        SliderSetting res;
        list.add(res = new SliderSetting(name, def, min, max, step, s));
        settings.defaults(name, def);
        rebuild();
        return res;
    }

    public void checkPref(String name, boolean def){
        list.add(new CheckSetting(name, def, null));
        settings.defaults(name, def);
        rebuild();
    }

    public void checkPref(String name, boolean def, Boolc changed){
        list.add(new CheckSetting(name, def, changed));
        settings.defaults(name, def);
        rebuild();
    }

    public void textPref(String name, String def){
        list.add(new TextSetting(name, def, null));
        settings.defaults(name, def);
        rebuild();
    }

    public void textPref(String name, String def, Cons<String> changed){
        list.add(new TextSetting(name, def, changed));
        settings.defaults(name, def);
        rebuild();
    }

    public void areaTextPref(String name, String def){
        list.add(new AreaTextSetting(name, def, null));
        settings.defaults(name, def);
        rebuild();
    }

    public void areaTextPref(String name, String def, Cons<String> changed){
        list.add(new AreaTextSetting(name, def, changed));
        settings.defaults(name, def);
        rebuild();
    }

    public void rebuild(){
        clearChildren();

        for(Setting setting : list){
            setting.add(this);
        }

        button(Core.bundle.get("settings.reset", "Reset to Defaults"), () -> {
            for(Setting setting : list){
                if(setting.name == null || setting.title == null) continue;
                settings.remove(setting.name);
            }
            rebuild();
        }).margin(14).width(240f).pad(6);
    }

    public abstract class Setting{
        public String name;
        public String title;
        public @Nullable String description;

        public Setting(String name){
            this.name = name;
            String winkey = "setting." + name + ".name.windows";
            title = OS.isWindows && bundle.has(winkey) ? bundle.get(winkey) : bundle.get("setting." + name + ".name", name);
            description = bundle.getOrNull("setting." + name + ".description");
        }

        public abstract void add(CopperSettingsTable table);

        public void addDesc(Element elem){
            Vars.ui.addDescTooltip(elem, description);
        }
    }

    public class CheckSetting extends Setting{
        boolean def;
        Boolc changed;

        public CheckSetting(String name, boolean def, Boolc changed){
            super(name);
            this.def = def;
            this.changed = changed;
        }

        @Override
        public void add(CopperSettingsTable table){
            CheckBox box = new CheckBox(title);

            box.update(() -> box.setChecked(settings.getBool(name)));

            box.changed(() -> {
                settings.put(name, box.isChecked());
                if(changed != null){
                    changed.get(box.isChecked());
                }
            });

            box.left();
            addDesc(table.add(box).left().padTop(3f).get());
            table.row();
        }
    }

    public class SliderSetting extends Setting{
        int def, min, max, step;
        SettingsMenuDialog.StringProcessor sp;

        public SliderSetting(String name, int def, int min, int max, int step, SettingsMenuDialog.StringProcessor s){
            super(name);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sp = s;
        }

        @Override
        public void add(CopperSettingsTable table){
            Slider slider = new Slider(min, max, step, false);

            slider.setValue(settings.getInt(name));

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();
            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                settings.put(name, (int)slider.getValue());
                value.setText(sp.get((int)slider.getValue()));
            });

            slider.change();

            addDesc(table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).left().padTop(4f).get());
            table.row();
        }
    }

    public class TextSetting extends Setting{
        String def;
        Cons<String> changed;

        public TextSetting(String name, String def, Cons<String> changed){
            super(name);
            this.def = def;
            this.changed = changed;
        }

        @Override
        public void add(CopperSettingsTable table){
            TextField field = new TextField();

            field.update(() -> field.setText(settings.getString(name)));

            field.changed(() -> {
                settings.put(name, field.getText());
                if(changed != null){
                    changed.get(field.getText());
                }
            });

            Table prefTable = table.table().left().padTop(3f).get();
            prefTable.add(field);
            prefTable.label(() -> title);
            addDesc(prefTable);
            table.row();
        }
    }

    public class AreaTextSetting extends TextSetting{
        public AreaTextSetting(String name, String def, Cons<String> changed){
            super(name, def, changed);
        }

        @Override
        public void add(CopperSettingsTable table){
            TextArea area = new TextArea("");
            area.setPrefRows(5);

            area.update(() -> {
                area.setText(settings.getString(name));
                area.setWidth(table.getWidth());
            });

            area.changed(() -> {
                settings.put(name, area.getText());
                if(changed != null){
                    changed.get(area.getText());
                }
            });

            addDesc(table.label(() -> title).left().padTop(3f).get());
            table.row().add(area).left();
            table.row();
        }
    }
}

