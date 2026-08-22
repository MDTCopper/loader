package copper.loader.func;

/**
 * A two-argument consumer that may throw.
 * Analogous to {@link java.util.function.BiConsumer}, but allows checked exceptions.
 */
public interface ThrowableCons2<T, N> {
    void get(T var1, N var2) throws Throwable;
}