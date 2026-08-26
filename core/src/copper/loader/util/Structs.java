package copper.loader.util;

import copper.loader.func.*;
import java.util.*;

/**
 * A utility class with generic array/collection helpers.
 */
public class Structs {
    /**
     * Finds the first element matching a predicate.
     *
     * @return the first matching element, or {@code null} if none found
     */
    public static <T> T find(T[] array, Boolf<T> value){
        for(T t : array){
            if(value.get(t)) return t;
        }
        return null;
    }

    /**
     * Checks whether any element matches a predicate.
     */
    public static <T> boolean contains(T[] array, Boolf<T> value){
        return find(array, value) != null;
    }

    /** Swaps two elements in an ArrayList. */
    public static <T> void swap(List<T> arr, int a, int b) {
        T t = arr.get(a);
        arr.set(a, arr.get(b));
        arr.set(b, t);
    }

    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(Func<? super T, ? extends U> keyExtractor){
        return (c1, c2) -> keyExtractor.get(c1).compareTo(keyExtractor.get(c2));
    }
}
