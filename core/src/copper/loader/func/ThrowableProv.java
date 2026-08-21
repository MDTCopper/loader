package copper.loader.func;

public interface ThrowableProv<T> {
    T get() throws Throwable;
}
