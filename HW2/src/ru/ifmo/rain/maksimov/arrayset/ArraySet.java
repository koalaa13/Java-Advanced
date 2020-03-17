package ru.ifmo.rain.maksimov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final Comparator<? super T> comparator;
    private final List<T> data;

    public static class ReversedList<U> extends AbstractList<U> {
        private boolean reversed;
        private final List<U> data;

        public ReversedList(List<U> data) {
            this.data = data;
        }

        public void reverse() {
            reversed = !reversed;
        }

        @Override
        public U get(int index) {
            return reversed ? data.get(size() - 1 - index) : data.get(index);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends T> data) {
        this(data, null);
    }

    public ArraySet(Collection<? extends T> data, Comparator<? super T> comparator) {
        this.comparator = comparator;
        Set<T> tmp = new TreeSet<>(comparator);
        tmp.addAll(data);
        this.data = new ArrayList<>(tmp);
    }

    private ArraySet(List<T> data, Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.data = data;
        if (data instanceof ReversedList ) {
            ((ReversedList) data).reverse();
        }
    }

    private void checkNonEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("Set is empty, can't get a element");
        }
    }

    private boolean validInd(int ind) {
        return 0 <= ind && ind < size();
    }

    private int getInd(T t, int found, int notFound) {
        int ind = Collections.binarySearch(data, t, comparator);
        if (ind < 0) {
            ind = -ind - 1;
            return validInd(ind + notFound) ? ind + notFound : -1;
        }
        return validInd(ind + found) ? ind + found : -1;
    }

    private T getElem(int ind) {
        return validInd(ind) ? data.get(ind) : null;
    }

    private int getLowerInd(T t) {
        return getInd(t, -1, -1);
    }

    @Override
    public T lower(T t) {
        return getElem(getLowerInd(t));
    }

    private int getFloorInd(T t) {
        return getInd(t, 0, -1);
    }

    @Override
    public T floor(T t) {
        return getElem(getFloorInd(t));
    }

    private int getCeilingInd(T t) {
        return getInd(t, 0, 0);
    }

    @Override
    public T ceiling(T t) {
        return getElem(getCeilingInd(t));
    }

    private int getHigherInd(T t) {
        return getInd(t, 1, 0);
    }

    @Override
    public T higher(T t) {
        return getElem(getHigherInd(t));
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) o, comparator) >= 0;
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
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (comparator == null) {
            if (fromElement instanceof Comparable && toElement instanceof Comparable) {
                if (((Comparable) fromElement).compareTo(toElement) > 0) {
                    throw new IllegalArgumentException("fromElement >= toElement");
                }
            } else {
                throw new IllegalArgumentException("fromElement and toElement aren't comparable");
            }
        } else {
            if (comparator.compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException("fromElement >= toElement");
            }
        }
        NavigableSet<T> pref = headSet(toElement, toInclusive);
        return pref.tailSet(fromElement, fromInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int bound = inclusive ? getFloorInd(toElement) : getLowerInd(toElement);
        return new ArraySet<>((bound == -1 ? Collections.emptyList() : data.subList(0, bound + 1)), comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int bound = inclusive ? getCeilingInd(fromElement) : getHigherInd(fromElement);
        return new ArraySet<>((bound == -1 ? Collections.emptyList() : data.subList(bound, size())), comparator);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
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
        checkNonEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkNonEmpty();
        return data.get(size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}
