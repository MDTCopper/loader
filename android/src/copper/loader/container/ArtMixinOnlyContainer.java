package copper.loader.container;

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
