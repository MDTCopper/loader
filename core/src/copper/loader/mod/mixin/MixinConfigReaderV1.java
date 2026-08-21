package copper.loader.mod.mixin;

import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.*;
import copper.loader.util.*;

/**
 * V1 format reader for Copper mixin configuration files.
 *
 * <p>The config JSON schema uses version filter strings as keys and mixin class names
 * (or arrays of names) as values. This reader evaluates each key against the target
 * {@link Version}, collects all matching mixin entries, and merges them into a single
 * {@code "mixins"} array.</p>
 *
 * <h3>Config JSON schema (version 1)</h3>
 *
 * <pre>{@code
 * {
 *     "version": 1,
 *     "config": {
 *         "required": true,
 *         "minVersion": "0.8",
 *         "package": "author.modname.mypatch",
 *         "mixins": {
 *             "*": [
 *                 "PatchClassA",
 *                 "OtherPatch"
 *             ],
 *             "<8.0.27179": "MyBackportClass"
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The {@code "mixins"} object maps version filter strings to mixin entries.
 * Version filter syntax follows {@link SemanticVersionFilter} for semantic expressions
 * and falls back to {@link StringVersionFilter} for exact string matching.
 * The special key {@code "*"} always matches.</p>
 */
public class MixinConfigReaderV1 implements IMixinConfigReader {
    @Override
    public MixinInfo read(Container container, Version version, Jval obj) {
        Jval.JsonMap mixins = obj.remove("mixins").asObject();
        Jval mergedMixins = Jval.newArray();
        MixinInfo info = new MixinInfo();

        for (var entry : mixins.entrySet()) {
            IVersionFilter filter = null;
            String ver = entry.getKey().trim();
            try {
                filter = new SemanticVersionFilter(ver);
            } catch (Throwable e) {
                filter = new StringVersionFilter(ver);
            }

            if (filter.check(version)) {
                Jval value = entry.getValue();
                if (value.isArray()) {
                    // Array entries are merged individually (deduplicated)
                    for (var v : value.asArray()) {
                        if (!mergedMixins.asArray().contains(v)) {
                            mergedMixins.add(v);
                            info.mixinName.add(v.asString());
                        }
                    }
                } else if (!mergedMixins.asArray().contains(value)) {
                    mergedMixins.add(value);
                    info.mixinName.add(value.asString());
                }
            }
        }

        obj.add("mixins", mergedMixins);
        info.container = container;
        info.config = obj.toString(Jval.Jformat.plain);
        info.packageName = obj.getString("package");
        return info;
    }
}
