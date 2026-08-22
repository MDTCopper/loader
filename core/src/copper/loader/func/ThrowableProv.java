package copper.loader.func;

/**
 * A supplier that may throw.
 * Analogous to {@link java.util.function.Supplier}, but allows checked exceptions.
 */
public interface ThrowableProv<T> {
    T get() throws Throwable;
}