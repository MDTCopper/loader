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
 *         "repo":         "user/repo",
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
 *             "exclude author.modname.internal.*"
 *             "include author.modname.*",
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
 *             "mindustry": "mixins/mindustry.json",
 *             "some:mod": "mixins/some.json"
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Field reference</h3>
 *
 * <h4>Required fields</h4>
 *
 * <h5>{@code id}</h5>
 * <ul>
 *   <li>Type: String</li>
 *   <li>Mod identifier in {@code author:name} format. Must contain exactly one colon ({@code :}).
 *       Allowed characters follow Java package naming rules: letters, digits, and underscores,
 *       separated by a single colon. Forbidden: spaces, dots ({@code .}), hyphens ({@code -}), commas ({@code ,}).</li>
 * </ul>
 *
 * <h5>{@code name}</h5>
 * <ul><li>Type: String</li><li>Human-readable display name.</li></ul>
 *
 * <h5>{@code author}</h5>
 * <ul><li>Type: String</li><li>Mod author name.</li></ul>
 *
 * <h5>{@code main}</h5>
 * <ul>
 *   <li>Type: String</li>
 *   <li>Fully qualified main class name. Must start with the {@code id} with colons
 *       replaced by dots (e.g., {@code "author:mod"} → must start with {@code "author.mod."}).</li>
 * </ul>
 *
 * <h5>{@code version}</h5>
 * <ul><li>Type: String</li><li>Semantic version ({@code X.Y.Z}). See {@link SemanticVersion}.</li></ul>
 *
 * <h4>Optional fields</h4>
 *
 * <h5>{@code description}</h5>
 * <ul><li>Type: String</li><li>Default: {@code ""}</li><li>Short description of the mod.</li></ul>
 *
 * <h5>{@code hidden}</h5>
 * <ul>
 *   <li>Type: Boolean</li>
 *   <li>Default: {@code false}</li>
 *   <li>Whether this mod is hidden. Hidden mods are server-side or client-side only
 *       and cannot register new content (blocks, items, etc.) and network packet.</li>
 * </ul>
 *
 * <h5>{@code repo}</h5>
 * <ul><li>Type: String</li><li>Default: {@code ""}</li><li>URL to the mod's repository or homepage.</li></ul>
 *
 * <h5>{@code extra}</h5>
 * <ul>
 *   <li>Type: Object</li>
 *   <li>Default: {@code {}}</li>
 *   <li>Additional fields for the game's ModMeta (e.g. {@code subtitle}).
 *       Deserialized by copper.core.mod.LoadedCopperMod when bridging into the game's mod registry.</li>
 * </ul>
 *
 * <h5>{@code dependencies}</h5>
 * <ul><li>Type: Object</li><li>Map of mod id → version filter (see below).</li></ul>
 *
 * <h5>{@code conflicts}</h5>
 * <ul><li>Type: Object</li><li>Map of mod id → version filter. If a conflicting mod
 *       is present with a matching version, loading is rejected.</li></ul>
 *
 * <h5>{@code exports}</h5>
 * <ul>
 *   <li>Type: Array of String</li>
 *   <li>Class visibility rules for this mod's own classes, matched top to bottom.
 *       Each entry is {@code "include <pattern>"} or {@code "exclude <pattern>"}
 *       using wildcard patterns. Rules are checked in order: to hide internal packages, place
 *       {@code "exclude"} before {@code "include"} (e.g. {@code exclude author.m.internal.*}
 *       then {@code include author.m.*}). The system automatically appends
 *       {@code "include author.modname.*"} at the end.</li>
 * </ul>
 *
 * <h5>{@code imports}</h5>
 * <ul>
 *   <li>Type: Object</li>
 *   <li>Map of dependency mod id → rule(s) granting extra class visibility into that dependency.
 *       Rules are matched top to bottom, same as exports.</li>
 * </ul>
 *
 * <h5>{@code mixins}</h5>
 * <ul><li>Type: Object</li><li>Map of target id → mixin config descriptor (see below).</li></ul>
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
 * <p>Each entry in {@code "mixins"} maps a target container id (e.g. {@code "mindustry"})
 * to a mixin config file path inside {@code assets/copper/}. The config file itself is
 * read by {@link copper.loader.mod.mixin.IMixinConfigReader} and uses a version-keyed
 * format for filtering mixin entries by target version.</p>
 *
 * <h3>Special mod ids</h3>
 * <p>{@code "mindustry"} refers to the game itself, and
 * {@code "loader"} refers to the Copper loader. Both can be used in
 * {@code "dependencies"} and {@code "conflicts"}, but only {@code "mindustry"}
 * is a valid mixin target — you cannot apply mixins to the loader.</p>
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
            mod.dependencies.addAll(parseModDescriptors(meta.get("dependencies").asObject()));
        if (meta.containsKey("conflicts"))
            mod.dependencies.addAll(parseModDescriptors(meta.get("conflicts").asObject()));

        if (meta.containsKey("exports")) {
            // Export rules matched top to bottom; system auto-appends "include author.modname.*"
            Jval.JsonArray exports = meta.get("exports").asArray();
            for (var item : exports)
                mod.exportRules.add(item.asString());
        }
        mod.exportRules.add("include " + mod.id.replace(':', '.') + ".*");

        if (meta.containsKey("imports")) {
            // Import rules matched top to bottom for each dependency mod
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
                mod.importRules.put(entry.getKey(), arr);
            }
        }

        if (meta.containsKey("mixins")) {
            // Map target container id → mixin config path; the config file is read by IMixinConfigReader
            Jval.JsonMap mixins = meta.get("mixins").asObject();
            for (var entry : mixins.entrySet()) {
                String id = entry.getKey();
                Jval conf = entry.getValue();
                MixinDescriptor desc = new MixinDescriptor();
                desc.id = id;
                desc.configPath = conf.asString();
                mod.mixins.add(desc);
            }
        }
    }

    private ArrayList<RelationDescriptor> parseModDescriptors(Jval.JsonMap obj) {
        ArrayList<RelationDescriptor> descs = new ArrayList<>();
        for (var entry : obj.entrySet()) {
            String id = entry.getKey().trim();
            Jval ver = entry.getValue();
            RelationDescriptor desc = new RelationDescriptor();
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
