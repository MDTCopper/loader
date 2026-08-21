package copper.loader.func;

public interface ThrowableCons<T> {
    void get(T obj) throws Throwable;
}
