package copper.loader.func;

/**
 * A two-argument consumer.
 * Analogous to {@link java.util.function.BiConsumer}.
 */
public interface Cons2<T, N> {
    void get(T var1, N var2);
}
