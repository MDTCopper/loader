package copper.loader.util;

import copper.loader.func.*;
import java.util.*;

public class Structs {
    public static <T> T find(T[] array, Boolf<T> value){
        for(T t : array){
            if(value.get(t)) return t;
        }
        return null;
    }

    public static <T> boolean contains(T[] array, Boolf<T> value){
        return find(array, value) != null;
    }

    public static <T> void swap(ArrayList<T> arr, int a, int b) {
        T t = arr.get(a);
        arr.set(a, arr.get(b));
        arr.set(b, t);
    }
}
