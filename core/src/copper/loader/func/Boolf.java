package copper.loader.func;

/**
 * A predicate (boolean-valued function) on a single argument.
 * Analogous to {@link java.util.function.Predicate}, but without the Java functional interface convention.
 */
public interface Boolf<T> {
    boolean get(T var1);
}
