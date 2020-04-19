package ru.ifmo.rain.bobrov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class FastReverseList<T> extends AbstractList<T> {
    private final List<T> list;
    private boolean reverse;

    FastReverseList(List<T> newElements) {
        list = newElements;
        reverse = false;
    }

    @Override
    public T get(int index) {
        return list.get(reverse ? size() - index - 1 : index);
    }

    @Override
    public int size() {
        return list.size();
    }

    void reverse() {
        reverse = !reverse;
    }
}
