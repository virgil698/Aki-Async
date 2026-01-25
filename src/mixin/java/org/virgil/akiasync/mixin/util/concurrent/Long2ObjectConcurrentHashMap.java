package org.virgil.akiasync.mixin.util.concurrent;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.concurrent.ConcurrentHashMap;

public class Long2ObjectConcurrentHashMap<V> implements Long2ObjectMap<V> {
    private final ConcurrentHashMap<Long, V> map = new ConcurrentHashMap<>();

    @Override
    public V put(long key, V value) {
        return map.put(key, value);
    }

    @Override
    public V get(long key) {
        return map.get(key);
    }

    @Override
    public V remove(long key) {
        return map.remove(key);
    }

    @Override
    public boolean containsKey(long key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void putAll(java.util.Map<? extends Long, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public LongSet keySet() {

        return new LongSet() {
            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public boolean contains(long key) {
                return map.containsKey(key);
            }

            @Override
            public boolean add(long key) {
                return map.putIfAbsent(key, null) == null;
            }

            @Override
            public boolean remove(long key) {
                return map.remove(key) != null;
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public LongIterator iterator() {
                return new LongIterator() {
                    private final java.util.Iterator<Long> it = map.keySet().iterator();

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

            @Override
            public boolean retainAll(LongCollection c) {
                boolean modified = false;
                java.util.Iterator<Long> it = map.keySet().iterator();
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

            public long[] toLongArray() {
                long[] array = new long[map.size()];
                int i = 0;
                for (long l : map.keySet()) {
                    array[i++] = l;
                }
                return array;
            }

            public long[] toLongArray(long[] a) {
                long[] array = a.length >= map.size() ? a : new long[map.size()];
                int i = 0;
                for (long l : map.keySet()) {
                    array[i++] = l;
                }
                return array;
            }

            public long[] toArray(long[] a) {
                return toLongArray(a);
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
                java.util.Iterator<Long> it = map.keySet().iterator();
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
                for (long l : map.keySet()) {
                    if (!that.contains(l)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public int hashCode() {
                int result = 0;
                for (long l : map.keySet()) {
                    result += (int) (l ^ (l >>> 32));
                }
                return result;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (long l : map.keySet()) {
                    if (!first) sb.append(", ");
                    sb.append(l);
                    first = false;
                }
                sb.append("]");
                return sb.toString();
            }
        };
    }

    @Override
    public ObjectCollection<V> values() {
        return new ObjectCollection<V>() {
            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return map.containsValue(o);
            }

            @Override
            public ObjectIterator<V> iterator() {
                return new ObjectIterator<V>() {
                    private final java.util.Iterator<V> it = map.values().iterator();

                    @Override
                    public V next() {
                        return it.next();
                    }

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return map.values().toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return map.values().toArray(a);
            }

            @Override
            public boolean add(V v) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                return map.values().remove(o);
            }

            @Override
            public boolean containsAll(java.util.Collection<?> c) {
                return map.values().containsAll(c);
            }

            @Override
            public boolean addAll(java.util.Collection<? extends V> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return map.values().removeAll(c);
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return map.values().retainAll(c);
            }

            @Override
            public void clear() {
                map.clear();
            }
        };
    }

    @Override
    public ObjectSet<Long2ObjectMap.Entry<V>> long2ObjectEntrySet() {
        return new ObjectSet<Long2ObjectMap.Entry<V>>() {
            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return map.entrySet().contains(o);
            }

            @Override
            public ObjectIterator<Long2ObjectMap.Entry<V>> iterator() {
                return new ObjectIterator<Long2ObjectMap.Entry<V>>() {
                    private final java.util.Iterator<java.util.Map.Entry<Long, V>> it = map.entrySet().iterator();

                    @Override
                    public Long2ObjectMap.Entry<V> next() {
                        java.util.Map.Entry<Long, V> entry = it.next();
                        return new Long2ObjectMap.Entry<V>() {
                            @Override
                            public long getLongKey() {
                                return entry.getKey();
                            }

                            @Override
                            public V getValue() {
                                return entry.getValue();
                            }

                            @Override
                            public V setValue(V value) {
                                return entry.setValue(value);
                            }
                        };
                    }

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return map.entrySet().toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return map.entrySet().toArray(a);
            }

            @Override
            public boolean add(Long2ObjectMap.Entry<V> entry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                return map.entrySet().remove(o);
            }

            @Override
            public boolean containsAll(java.util.Collection<?> c) {
                return map.entrySet().containsAll(c);
            }

            @Override
            public boolean addAll(java.util.Collection<? extends Long2ObjectMap.Entry<V>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return map.entrySet().removeAll(c);
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return map.entrySet().retainAll(c);
            }

            @Override
            public void clear() {
                map.clear();
            }
        };
    }

    @Override
    public V defaultReturnValue() {
        return null;
    }

    @Override
    public void defaultReturnValue(V rv) {

    }
}
