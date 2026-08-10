package copper.loader.mod.meta;

import copper.loader.mod.*;
import copper.loader.util.*;
import java.util.*;

/**
 * V1 format reader for Copper mod metadata ({@code copper.mod.json} / {@code copper.mod.hjson}).
 *
 * <h2>Meta JSON schema (version 1)</h2>
 *
 * <p>The top-level JSON/HJSON must contain a {@code "version"} field and a {@code "meta"} object.
 * The {@code "meta"} object has the structure described below.</p>
 *
 * <pre>{@code
 * {
 *     "version": 1,
 *     "meta": {
 *         // ── Required fields ──────────────────────────────────────
 *
 *         "id":      "author:modname",
 *         "name":    "My Mod",
 *         "author":  "Author Name",
 *         "main":    "author.modname.MyMainClass",
 *         "version": "1.0.0",
 *
 *         // ── Optional fields ──────────────────────────────────────
 *
 *         "description": "A short description of the mod.",
 *         "hidden":       true,
 *         "repo":         "https://github.com/user/repo",
 *         "extra":        {},
 *
 *         // ── Dependencies & conflicts ─────────────────────────────
 *
 *         "dependencies": {
 *             "some:mod": ">=1.0.0",
 *             "other:mod": ["3.2.1", "4.0.0"]
 *         },
 *         "conflicts": {
 *             "bad:mod": "*"
 *         },
 *
 *         // ── Class export / import rules ──────────────────────────
 *
 *         "exports": [
 *             "include author.modname.*",
 *             "exclude author.modname.internal.*"
 *         ],
 *         "imports": {
 *             "dep:mod":  "include dep.*",
 *             "other:dep": [
 *                 "include other.sub.*",
 *                 "include other.extra.*"
 *             ]
 *         },
 *
 *         // ── Mixin configurations ─────────────────────────────────
 *
 *         "mixins": {
 *             "mindustry": "mixins.mindustry.json",
 *             "some:mod": {
 *                 "version": ">=1.0.0 && <2.0.0",
 *                 "path":    "mixins.some.json"
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Field reference</h3>
 *
 * <table>
 *   <caption>Required fields</caption>
 *   <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 *   <tr><td>{@code id}</td><td>String</td><td>Mod identifier in {@code author:name} format. Must contain exactly one colon ({@code :}).<br>
 *       Allowed characters follow Java package naming rules: letters, digits, and underscores,
 *       separated by a single colon. Forbidden: spaces, dots ({@code .}), hyphens ({@code -}), commas ({@code ,}).<br>
 *       </td></tr>
 *   <tr><td>{@code name}</td><td>String</td><td>Human-readable display name.</td></tr>
 *   <tr><td>{@code author}</td><td>String</td><td>Mod author name.</td></tr>
 *   <tr><td>{@code main}</td><td>String</td><td>Fully qualified main class name. Must start with the {@code id} with colons
 *       replaced by dots (e.g., {@code "author:mod"} → must start with {@code "author.mod."}).</td></tr>
 *   <tr><td>{@code version}</td><td>String</td><td>Semantic version ({@code X.Y.Z}). See {@link SemanticVersion}.</td></tr>
 * </table>
 *
 * <table>
 *   <caption>Optional fields</caption>
 *   <tr><th>Field</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>{@code description}</td><td>String</td><td>{@code ""}</td><td>Short description of the mod.</td></tr>
 *   <tr><td>{@code hidden}</td><td>Boolean</td><td>{@code false}</td><td>Whether this mod is hidden. Hidden mods are server-side or client-side
 *       only and cannot register new content (blocks, items, etc.).</td></tr>
 *   <tr><td>{@code repo}</td><td>String</td><td>{@code ""}</td><td>URL to the mod's repository or homepage.</td></tr>
 *   <tr><td>{@code extra}</td><td>Object</td><td>{@code {}}</td><td>Additional fields for the game's ModMeta
 *       (e.g. {@code subtitle}). Deserialized by copper.core.mod.LoadedCopperMod
 *       when bridging into the game's mod registry.</td></tr>
 *   <tr><td>{@code dependencies}</td><td>Object</td><td>—</td><td>Map of mod id → version filter (see below).</td></tr>
 *   <tr><td>{@code conflicts}</td><td>Object</td><td>—</td><td>Map of mod id → version filter. If a conflicting mod
 *       is present with a matching version, loading is rejected.</td></tr>
 *   <tr><td>{@code exports}</td><td>Array of String</td><td>—</td><td>Class visibility rules for this mod's own classes.
 *       Each entry is {@code "include <pattern>"} or {@code "exclude <pattern>"} using wildcard patterns.</td></tr>
 *   <tr><td>{@code imports}</td><td>Object</td><td>—</td><td>Map of dependency mod id → rule(s) granting extra
 *       class visibility into that dependency.</td></tr>
 *   <tr><td>{@code mixins}</td><td>Object</td><td>—</td><td>Map of target id → mixin config descriptor (see below).</td></tr>
 * </table>
 *
 * <h3>Version filter syntax (used in {@code dependencies}, {@code conflicts}, and mixin {@code version})</h3>
 *
 * <table>
 *   <caption>Value types</caption>
 *   <tr><th>Format</th><th>Example</th><th>Behavior</th></tr>
 *   <tr><td>Single string</td><td>{@code ">=1.0.0"}</td><td>Parsed as a {@link SemanticVersionFilter} with full expression support.
 *       If parsing fails, falls back to {@link StringVersionFilter} (exact string match).</td></tr>
 *   <tr><td>Array of strings</td><td>{@code ["1.0.0", "2.0.0"]}</td><td>Treated as an exact-match list ({@link StringVersionFilter}).</td></tr>
 * </table>
 *
 * <table>
 *   <caption>Semantic version filter expression syntax</caption>
 *   <tr><th>Syntax</th><th>Example</th><th>Meaning</th></tr>
 *   <tr><td>Exact</td><td>{@code 1.0.0}</td><td>Matches exactly version {@code 1.0.0}.</td></tr>
 *   <tr><td>Wildcard</td><td>{@code 1.*.0}, {@code 1.x.0}</td><td>{@code *} or {@code x} matches any number for that component.</td></tr>
 *   <tr><td>Comparison</td><td>{@code >=1.0.0}, {@code <=2.0.0}, {@code >1.0.0}, {@code <2.0.0}, {@code =1.0.0}, {@code !=1.0.0}</td>
 *       <td>Standard version comparison.</td></tr>
 *   <tr><td>Caret</td><td>{@code ^1.2.3}</td><td>Allows changes that do not modify the leftmost non-zero digit
 *       (compatible releases, per semver convention).</td></tr>
 *   <tr><td>Tilde</td><td>{@code ~1.2.3}</td><td>Allows patch-level changes within the same minor version
 *       ({@code >=1.2.3 && 1.2.x}).</td></tr>
 *   <tr><td>Range</td><td>{@code 1.0.0 - 2.0.0}</td><td>Equivalent to {@code >=1.0.0 && <=2.0.0}.</td></tr>
 *   <tr><td>AND</td><td>{@code >=1.0.0 && <2.0.0}</td><td>Both expressions must match.</td></tr>
 *   <tr><td>OR</td><td>{@code 1.0.0 || 1.2.0}</td><td>Either expression must match.</td></tr>
 *   <tr><td>Grouping</td><td>{@code (>=1.0.0 && <2.0.0) || 3.0.0}</td><td>Parentheses for precedence.</td></tr>
 *   <tr><td>Implicit AND</td><td>{@code >=1.0.0 <2.0.0}</td><td>Adjacent expressions without an explicit operator are ANDed together.</td></tr>
 * </table>
 *
 * <h3>Mixin descriptor formats</h3>
 *
 * <p>Each entry in {@code "mixins"} can be written in one of two forms:</p>
 *
 * <table>
 *   <caption>Mixin config formats</caption>
 *   <tr><th>Format</th><th>Example</th><th>Description</th></tr>
 *   <tr><td>String shorthand</td>
 *       <td>{@code "mindustry": "mixins.my.json"}</td>
 *       <td>Mixin config file path only. Version defaults to {@code *} (always applies).</td></tr>
 *   <tr><td>Object form</td>
 *       <td>{@code "mindustry": {"version": ">=146", "path": "mixins.my.json"}}</td>
 *       <td>Full descriptor with optional {@code version} filter (default {@code *})
 *           and required {@code path} to the mixin config file inside {@code assets/copper/}.</td></tr>
 * </table>
 */
public class MetaReaderV1 implements IMetaReader {
    public void read(Mod mod, Jval obj) {
        Jval.JsonMap meta = obj.asObject();

        // ── Required fields ────────────────────────────────────────────

        mod.id = meta.get("id").asString().trim();
        mod.name = meta.get("name").asString();
        mod.author = meta.get("author").asString();
        mod.main = meta.get("main").asString().trim();
        mod.version = new SemanticVersion(meta.get("version").asString());

        if (mod.id.split(":").length != 2 || mod.id.contains(" ") || mod.id.contains(".") || mod.id.contains("-") || mod.id.contains(",") ||
                mod.id.startsWith("mindustry:") || mod.id.equals("mindustry") || mod.id.startsWith("loader:") || mod.id.equals("loader"))
            throw new RuntimeException("invalid copper mod id: " + mod.id);

        if (!mod.main.startsWith(mod.id.replace(':', '.') + "."))
            throw new RuntimeException("invalid copper main class in mod " + mod.id + " : " + mod.main);

        // ── Optional fields ────────────────────────────────────────────

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
