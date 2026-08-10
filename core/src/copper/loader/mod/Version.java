package copper.loader.mod;

/**
 * Abstract base for version objects.
 *
 * <p>Subclasses provide different version parsing strategies.
 * Equality comparison is delegated to {@link #equals(Version)}.</p>
 */
public abstract class Version {
    public String toString() {
        return "";
    }

    /**
     * Checks semantic equality with another version.
     */
    public abstract boolean equals(Version version);

    @Override
    public boolean equals(Object other) {
        if (other instanceof Version version)
            return equals(version);
        return false;
    }
}
