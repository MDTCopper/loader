package copper.loader.container;

/**
 * A {@link MixinContainer} used by the builder that only provides bytecode.
 *
 * <p>In the builder phase no class is ever actually loaded — the bytecode is
 * only read, transformed and dexed — so class loading always fails here.</p>
 */
public class ArtMixinOnlyContainer extends MixinContainer {
    // mixin only. never load class.
    @Override
    public Class<?> loadOwnClass(String name) {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }
}