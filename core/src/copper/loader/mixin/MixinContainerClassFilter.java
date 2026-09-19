package copper.loader.mixin;

import copper.loader.container.*;

/**
 * Pre-configured class filter for the mixin engine's isolated classloader.
 *
 * <p>The mixin engine runs inside a child-first classloader (see
 * copper.launch.JvmPlatform#createMixinEngine). This filter controls which
 * classes are loaded inside that isolated classloader and which are delegated to
 * the parent (system) classloader:</p>
 *
 * <ul>
 *   <li><b>Included</b> in the isolated classloader:
 *     <ul>
 *       <li>{@code org.spongepowered.asm.*} — the Mixin framework itself</li>
 *       <li>{@code copper.loader.mixin.*} — Copper's mixin integration classes
 *           ({@link MixinEngine}, {@link MixinEngineService}, etc.)</li>
 *     </ul>
 *   </li>
 *   <li><b>Excluded</b> (delegated to the parent system classloader):
 *     <ul>
 *       <li>{@code copper.loader.mixin.IMixinEngine} and this filter class itself —
 *           these are shared interfaces loaded by the system classloader so that
 *           containers can reference the mixin engine through the {@code IMixinEngine}
 *           interface without reflection.</li>
 *       <li>{@code copper.loader.*} — all other Copper loader classes. The mixin
 *           engine does not need them, and keeping them in the parent classloader
 *           avoids duplicate class definitions.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Any class that does not match a rule is loaded by the parent classloader
 * via normal delegation, ensuring the mixin engine can still access standard
 * library classes.</p>
 */
public class MixinContainerClassFilter extends ClassFilter {
    public MixinContainerClassFilter() {
        super();
        addRule("exclude copper.loader.mixin.IMixinEngine");
        addRule("exclude copper.loader.mixin.MixinContainerClassFilter");
        addRule("include org.spongepowered.asm.*");
        addRule("include copper.loader.mixin.*");
        addRule("exclude copper.loader.*");
    }
}
