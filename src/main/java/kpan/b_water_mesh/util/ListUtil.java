package kpan.b_water_mesh.util;

import java.util.List;
import java.util.function.Predicate;

public class ListUtil {

    public static <T> void swap(List<T> list, int a, int b) {
        T tmp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, tmp);
    }

    public static <T> int indexOf(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i)))
                return i;
        }
        return -1;
    }
}
