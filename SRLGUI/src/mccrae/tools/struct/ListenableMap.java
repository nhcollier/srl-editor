/* 
 * Copyright (c) 2008, National Institute of Informatics
 *
 * This file is part of SRL, and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June 1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://www.fsf.org/licensing/licenses/info/GPLv2.html.
 */
package mccrae.tools.struct;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author John McCrae
 */
public class ListenableMap<K, V> extends AbstractMap<K, V> {

    Map<K, V> map;
    List<CollectionChangeListener<V>> listeners;

    public ListenableMap(Map<K, V> map) {
        this.map = map;
        listeners = new LinkedList<CollectionChangeListener<V>>();
    }

    private void fireEvent(CollectionChangeEvent<V> e) {
        for (CollectionChangeListener listener : listeners) {
            listener.collectionChanged(e);
        }
    }

    public void addCollectionChangeListener(CollectionChangeListener<V> listener) {
        listeners.add(listener);
    }

    public void removeCollectionChangeListener(CollectionChangeListener<V> listener) {
        listeners.remove(listener);
    }

    @Override
    public V put(K key, V value) {
        V oldVal = map.get(key);
        V rv = map.put(key, value);
        fireEvent(new CollectionChangeEvent(oldVal, value, key));
        return rv;

    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }
    

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ListenableSet(map.entrySet());
    }

    private class ListenableMapEntrySet extends AbstractSet<Entry<K,V>> {

        Set<Entry<K,V>> set;
        public ListenableMapEntrySet(Set<Entry<K,V>> set) {
            this.set = set;
        }

        
        
        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean add(Entry<K,V> e) {
            boolean rv = set.add(e);
            fireEvent(new CollectionChangeEvent<V>(null, e.getValue(), e.getKey()));
            return rv;
        }

        @Override
        public Iterator<Entry<K,V>> iterator() {
            return new ListenableSetIterator(set.iterator());
        }
    }

    private class ListenableSetIterator implements Iterator<Entry<K,V>> {
        Iterator<Entry<K,V>> iter;
        Entry<K,V> last;

        ListenableSetIterator(Iterator<Entry<K,V>> iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Entry<K,V> next() {
            return last = iter.next();
        }

        public void remove() {
            iter.remove();
            fireEvent(new CollectionChangeEvent<V>(last.getValue(), null, last.getKey()));
        }
    }

    @Override
    public int size() {
        return map.size();
    }

}
