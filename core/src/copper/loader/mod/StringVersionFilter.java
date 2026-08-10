package copper.loader.mod;

import java.util.*;

/**
 * A version filter that matches exact version strings.
 *
 * <p>Useful for exact-version matching or as a fallback when
 * the version string cannot be parsed as a semantic version expression.</p>
 */
public class StringVersionFilter implements IVersionFilter {
    /** List of accepted version strings. */
    public ArrayList<String> versions;

    public StringVersionFilter(ArrayList<String> versions) {
        this.versions = versions;
    }

    public StringVersionFilter(String version) {
        this();
        versions.add(version.trim());
    }

    public StringVersionFilter() {
        this(new ArrayList<>());
    }

    @Override
    public boolean check(Version v) {
        String ver = v.toString().trim();
        for (String support : versions)
            if (support.trim().equals(ver))
                return true;
        return false;
    }
}
