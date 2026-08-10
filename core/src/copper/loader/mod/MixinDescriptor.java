package copper.loader.mod;

/**
 * A mixin configuration descriptor linking a target container to a mixin config file.
 */
public class MixinDescriptor {
    /** Target container id (e.g., {@code "mindustry"}, {@code "some:mod"}). */
    public String id;
    /** Version filter controlling when this mixin config is applied. */
    public IVersionFilter version;
    /** Path to the mixin config JSON inside {@code assets/copper/}. */
    public String configPath;
}
