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

/**
 *
 * @author John McCrae
 */
public class ListenableList<E> extends AbstractList<E> {
    List<E> list;
    List<CollectionChangeListener<E>> listeners;
    
    
    public ListenableList(List<E> list) {
        this.list = list;
        listeners = new LinkedList<CollectionChangeListener<E>>();
    }
    
    private void fireEvent(CollectionChangeEvent<E> e) {
        for(CollectionChangeListener<E> listener : listeners) {
            listener.collectionChanged(e);
        }
    }
    
    public void addCollectionChangeListener(CollectionChangeListener<E> listener) {
        listeners.add(listener);
    }
    
    public void removeCollectionChangeListener(CollectionChangeListener<E> listener) {
        listeners.remove(listener);
    }
    
    @Override
    public int size() {
        return list.size();
    }
    
    @Override
    public E set(int index, E element) {
        E oldVal = list.get(index);
        E rv = list.set(index, element);
        fireEvent(new CollectionChangeEvent<E>(oldVal,element,(Integer)index));
        return rv;
    }


    @Override
    public E remove(int index) {
        E oldVal = list.get(index);
        E rv = list.remove(index);
        fireEvent(new CollectionChangeEvent<E>(oldVal, null, (Integer)index));
        return rv;
    }

    @Override
    public boolean remove(Object o) {
        Integer index = list.indexOf(o);
        boolean rv = list.remove(o);
        fireEvent(new CollectionChangeEvent<E>((E)o, null, (Integer)index));
        return rv;
    }

    private class ListenableListIterator implements ListIterator<E> {
        ListIterator<E> iter;
        
        public ListenableListIterator(ListIterator<E> iter) {
            this.iter = iter;
        }
        
        
        public void add(E e) {
            iter.add(e);
            fireEvent(new CollectionChangeEvent<E>(null, e, (Integer)(iter.previousIndex()+1)));
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public boolean hasPrevious() {
            return iter.hasPrevious();
        }

        public E next() {
            return iter.next();
        }

        public int nextIndex() {
            return iter.nextIndex();
        }

        public E previous() {
            return iter.previous();
        }

        public int previousIndex() {
            return iter.previousIndex();
        }

        public void remove() {
            iter.previous();
            E elem = iter.next();
            iter.remove();
            fireEvent(new CollectionChangeEvent<E>(elem, null, (Integer)(iter.previousIndex()+1)));
        }

        public void set(E e) {
            iter.previous();
            E elem = iter.next();
            iter.set(e);
            fireEvent(new CollectionChangeEvent<E>(elem, e, new Integer(iter.previousIndex()+1)));
        }
        
    }
    
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListenableListIterator(list.listIterator(index));
    }


    @Override
    public Iterator<E> iterator() {
        return new ListenableListIterator(list.listIterator());
    }

    @Override
    public void add(int index, E element) {
        list.add(index, element);
        fireEvent(new CollectionChangeEvent<E>(null, element, (Integer)index));
    }

    @Override
    public boolean add(E e) {
        boolean rval = list.add(e);
        fireEvent(new CollectionChangeEvent<E>(null, e, (Integer)(list.size()-1)));
        return rval;
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }
}
