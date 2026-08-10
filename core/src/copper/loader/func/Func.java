package copper.loader.func;

/**
 * A single-argument function.
 * Analogous to {@link java.util.function.Function}.
 */
public interface Func<P, R> {
    R get(P var1);
}
