package copper.loader.mod;

/**
 * A version represented as an arbitrary string.
 *
 * <p>Used as a fallback when a version string does not conform to semantic versioning.</p>
 */
public class StringVersion extends Version {
    /** The raw version string. */
    public String version;

    /**
     * @param txt the version string (whitespace trimmed)
     */
    public StringVersion(String txt) {
        version = txt.trim();
    }

    @Override
    public String toString() {
        return version;
    }

    @Override
    public boolean equals(Version obj) {
        if (obj instanceof StringVersion ver)
            return ver.version.equals(version);
        return false;
    }
}
