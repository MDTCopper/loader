package copper.loader.mod.meta;

import copper.loader.mod.*;
import copper.loader.util.*;
import java.util.*;

public class MetaReaderV1 implements IMetaReader {
    public void read(Mod mod, Jval obj) {
        Jval.JsonMap meta = obj.asObject();

        // necessary info

        mod.id = meta.get("id").asString().trim();
        mod.name = meta.get("name").asString();
        mod.author = meta.get("author").asString();
        mod.main = meta.get("main").asString().trim();
        mod.version = new SemanticVersion(meta.get("version").asString());

        if (mod.id.split(":").length != 2 || mod.id.contains(" ") || mod.id.contains(".") || mod.id.contains("-") || mod.id.contains(",") ||
                mod.id.startsWith("mindustry:") || mod.id.equals("mindustry") || mod.id.startsWith("loader:") || mod.id.equals("loader"))
            throw new RuntimeException("invalid copper mod id: " + mod.id);

        if (!mod.main.startsWith(mod.id.replace(':', '.')))
            throw new RuntimeException("invalid copper main class in mod " + mod.id + " : " + mod.main);

        // optional info

        if (meta.containsKey("description"))
            mod.description = meta.get("description").asString();
        if (meta.containsKey("hidden"))
            mod.hidden = meta.get("hidden").asBool();
        if (meta.containsKey("repo"))
            mod.repo = meta.get("repo").asString();
        if (meta.containsKey("extra"))
            mod.extraMeta = meta.get("extra").toString(Jval.Jformat.plain);

        if (meta.containsKey("dependencies"))
            mod.dependency.addAll(parseModDescriptors(meta.get("dependencies").asObject()));
        if (meta.containsKey("conflicts"))
            mod.dependency.addAll(parseModDescriptors(meta.get("conflicts").asObject()));

        if (meta.containsKey("exports")) {
            Jval.JsonArray exports = meta.get("exports").asArray();
            for (var item : exports)
                mod.exportRule.add(item.asString());
        }

        if (meta.containsKey("imports")) {
            Jval.JsonMap imports = meta.get("imports").asObject();
            for (var entry : imports.entrySet()) {
                ArrayList<String> arr = new ArrayList<>();
                Jval val = entry.getValue();
                if (val.isString()) {
                    arr.add(val.asString());
                } else {
                    for (var item : val.asArray())
                        arr.add(item.asString());
                }
                mod.importRule.put(entry.getKey(), arr);
            }
        }

        if (meta.containsKey("mixins")) {
            Jval.JsonMap mixins = meta.get("mixins").asObject();
            for (var entry : mixins.entrySet()) {
                String id = entry.getKey();
                Jval conf = entry.getValue();
                MixinDescriptor desc = new MixinDescriptor();
                desc.id = id;
                if (conf.isString()) {
                    desc.version = new SemanticVersionFilter("*");
                    desc.configPath = conf.asString();
                } else if (conf.isObject()) {
                    String ver = conf.getString("version", "*").trim();
                    desc.configPath = conf.getString("path").trim();
                    try {
                        desc.version = new SemanticVersionFilter(ver);
                    } catch (Throwable e) {
                        desc.version = new StringVersionFilter(ver);
                    }
                }
                mod.mixin.add(desc);
            }
        }
    }

    private ArrayList<ModDescriptor> parseModDescriptors(Jval.JsonMap obj) {
        ArrayList<ModDescriptor> descs = new ArrayList<>();
        for (var entry : obj.entrySet()) {
            String id = entry.getKey().trim();
            Jval ver = entry.getValue();
            ModDescriptor desc = new ModDescriptor();
            desc.id = id;
            if (ver.isString()) {
                try {
                    desc.version = new SemanticVersionFilter(ver.asString());
                } catch (Throwable e) {
                    desc.version = new StringVersionFilter(ver.asString());
                }
            } else if (ver.isArray()) {
                var vers = ver.asArray();
                ArrayList<String> arr = new ArrayList<>();
                for (var v : vers)
                    arr.add(v.asString().trim());
                desc.version = new StringVersionFilter(arr);
            }
            descs.add(desc);
        }
        return descs;
    }
}
