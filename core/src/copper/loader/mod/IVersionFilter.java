package copper.loader.mod;

/**
 * A version filter that checks whether a version satisfies a constraint.
 */
public interface IVersionFilter {
    /**
     * @param v the version to test
     * @return {@code true} if the version is accepted by this filter
     */
    boolean check(Version v);
}
