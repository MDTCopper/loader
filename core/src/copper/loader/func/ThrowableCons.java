package copper.loader.func;

/**
 * A single-argument consumer that may throw.
 * Analogous to {@link java.util.function.Consumer}, but allows checked exceptions.
 */
public interface ThrowableCons<T> {
    void get(T obj) throws Throwable;
}