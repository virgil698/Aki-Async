package org.virgil.akiasync.mixin.util.concurrent;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentLongLinkedOpenHashSet implements LongSet {
    private final ConcurrentHashMap<Long, Boolean> map = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> order = new ConcurrentLinkedQueue<>();
    private final AtomicLong size = new AtomicLong(0);

    @Override
    public boolean add(long value) {
        Long key = value;
        if (map.putIfAbsent(key, Boolean.TRUE) == null) {
            order.add(key);
            size.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(long value) {
        Long key = value;
        if (map.remove(key) != null) {
            order.remove(key);
            size.decrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(long value) {
        return map.containsKey(value);
    }

    public long firstLong() {
        Long first = order.peek();
        return first != null ? first : Long.MIN_VALUE;
    }

    public long removeFirstLong() {
        Long first = order.poll();
        if (first != null) {
            map.remove(first);
            size.decrementAndGet();
            return first;
        }
        return Long.MIN_VALUE;
    }

    @Override
    public int size() {
        return (int) size.get();
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    @Override
    public void clear() {
        map.clear();
        order.clear();
        size.set(0);
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private final java.util.Iterator<Long> it = order.iterator();

            @Override
            public long nextLong() {
                return it.next();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

    public boolean addAll(LongSet c) {
        boolean modified = false;
        for (long l : c) {
            if (add(l)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean addAll(LongCollection c) {
        boolean modified = false;
        for (long l : c) {
            if (add(l)) {
                modified = true;
            }
        }
        return modified;
    }

    public boolean removeAll(LongSet c) {
        boolean modified = false;
        for (long l : c) {
            if (remove(l)) {
                modified = true;
            }
        }
        return modified;
    }

    public boolean retainAll(LongSet c) {
        boolean modified = false;
        java.util.Iterator<Long> it = order.iterator();
        while (it.hasNext()) {
            long l = it.next();
            if (!c.contains(l)) {
                remove(l);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(LongCollection c) {
        boolean modified = false;
        java.util.Iterator<Long> it = order.iterator();
        while (it.hasNext()) {
            long l = it.next();
            if (!c.contains(l)) {
                remove(l);
                modified = true;
            }
        }
        return modified;
    }

    public boolean containsAll(LongSet c) {
        for (long l : c) {
            if (!contains(l)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongCollection c) {
        for (long l : c) {
            if (!contains(l)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(LongCollection c) {
        boolean modified = false;
        for (long l : c) {
            if (remove(l)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public long[] toLongArray() {
        long[] array = new long[(int) size.get()];
        int i = 0;
        for (long l : order) {
            array[i++] = l;
        }
        return array;
    }

    public long[] toLongArray(long[] a) {
        long[] array = a.length >= size.get() ? a : new long[(int) size.get()];
        int i = 0;
        for (long l : order) {
            array[i++] = l;
        }
        return array;
    }

    public long[] toArray(long[] a) {
        return toLongArray(a);
    }

    public boolean add(long[] a) {
        boolean modified = false;
        for (long l : a) {
            if (add(l)) {
                modified = true;
            }
        }
        return modified;
    }

    public boolean remove(long[] a) {
        boolean modified = false;
        for (long l : a) {
            if (remove(l)) {
                modified = true;
            }
        }
        return modified;
    }

    public boolean contains(long[] a) {
        for (long l : a) {
            if (!contains(l)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object[] toArray() {
        long[] array = toLongArray();
        Object[] result = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        long[] array = toLongArray();
        if (a.length < array.length) {
            @SuppressWarnings("unchecked")
            T[] newArray = (T[]) new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                @SuppressWarnings("unchecked")
                T value = (T) Long.valueOf(array[i]);
                newArray[i] = value;
            }
            return newArray;
        }
        for (int i = 0; i < array.length; i++) {
            @SuppressWarnings("unchecked")
            T value = (T) Long.valueOf(array[i]);
            a[i] = value;
        }
        return a;
    }

    @Override
    public boolean addAll(java.util.Collection<? extends Long> c) {
        boolean modified = false;
        for (Long l : c) {
            if (add(l)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (o instanceof Long) {
                if (remove((Long) o)) {
                    modified = true;
                }
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        boolean modified = false;
        java.util.Iterator<Long> it = order.iterator();
        while (it.hasNext()) {
            long l = it.next();
            if (!c.contains(l)) {
                remove(l);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        for (Object o : c) {
            if (o instanceof Long) {
                if (!contains((Long) o)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongSet)) return false;
        LongSet that = (LongSet) o;
        if (size() != that.size()) return false;
        for (long l : order) {
            if (!that.contains(l)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (long l : order) {
            result += (int) (l ^ (l >>> 32));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (long l : order) {
            if (!first) sb.append(", ");
            sb.append(l);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
