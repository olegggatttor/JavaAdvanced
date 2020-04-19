package ru.ifmo.rain.bobrov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> set;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this((Comparator<T>) null);
    }


    public ArraySet(Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        this.comparator = comparator;
        Set<T> tmp = new TreeSet<>(comparator);
        tmp.addAll(collection);
        set = new ArrayList<>(tmp);
    }

    private ArraySet(List<T> collection, Comparator<? super T> comparator) {
        set = collection;
        if (collection instanceof FastReverseList) {
            ((FastReverseList) set).reverse();
        }
        this.comparator = comparator;
    }

    private boolean isInRange(int i) {
        return -1 < i && i < size();
    }

    private T getElement(int pos) {
        return (pos >= 0) ? set.get(pos) : null;
    }

    private int floorIndex(T element) {
        return findPos(element, 0, -1);
    }

    @Override
    public T floor(T element) {
        return getElement(floorIndex(element));
    }


    private int ceilingIndex(T element) {
        return findPos(element, 0, 0);
    }

    @Override
    public T ceiling(T element) {
        return getElement(ceilingIndex(element));
    }

    private int lowerIndex(T element) {
        return findPos(element, -1, -1);
    }

    @Override
    public T lower(T element) {
        return getElement(lowerIndex(element));
    }

    private int higherIndex(T element) {
        return findPos(element, 1, 0);
    }

    @Override
    public T higher(T element) {
        return getElement(higherIndex(element));
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new FastReverseList<>(set), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int l = fromInclusive ? ceilingIndex(fromElement) : higherIndex(fromElement);
        int r = toInclusive ? floorIndex(toElement) : lowerIndex(toElement);
        if (l == -1 || r == -1 || l > r) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(set.subList(l, r + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return this;
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return this;
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(set).iterator();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private int compare(T firstElement, T secondElement) {
        return (comparator == null) ? ((Comparable<T>) firstElement).compareTo(secondElement) :
                comparator.compare(firstElement, secondElement);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return headSet(toElement).tailSet(fromElement);

    }

    private int findPos(T element, int shiftFound, int shiftNotFound) {
        int pos = Collections.binarySearch(set, element, comparator);
        if (pos >= 0) {
            pos+=shiftFound;
            return isInRange(pos) ? pos : -1;
        }
        pos = -(pos + 1) + shiftNotFound;
        return isInRange(pos) ? pos : -1;
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return set.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return set.get(size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object object) {
        return Collections.binarySearch(set, (T) object, comparator) >= 0;
    }
}
