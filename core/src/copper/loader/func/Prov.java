package copper.loader.func;

/**
 * A supplier that provides a value.
 * Analogous to {@link java.util.function.Supplier}.
 */
public interface Prov<T> {
    T get();
}