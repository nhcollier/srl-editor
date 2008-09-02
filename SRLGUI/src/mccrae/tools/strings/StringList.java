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
package mccrae.tools.strings;
import java.io.*;
import java.util.*;

/**
 * Wraps a string buffer so that it appears as a list of characters.
 *
 * @author John McCrae, National Institute of Informatics
 */
public class StringList implements List<Character>, Serializable, Appendable, CharSequence, Comparable<CharSequence> {
    private StringBuffer str;
    
    /** Creates a new instance of StringList */
    public StringList(CharSequence s) {
        str = new StringBuffer(s);
    }
    
    public StringList(StringBuffer s) {
        str = s;
    }
    
    public void add(int i, Character c) {
        str = str.insert(i,c);
    }
    
    public boolean add(Character s) {
        str = str.append(s);
        return true;
    }
    
    private CharSequence charCollectionToString(Collection<? extends Character> chars) {
        StringBuffer sb = new StringBuffer(chars.size());
        int i = 0;
        for(Character c  : chars) {
            sb.setCharAt(i++,c.charValue());
        }
        return sb;
    }
    
    public boolean addAll(int i, Collection<? extends Character> chars) {
        str = str.insert(i,charCollectionToString(chars));
        return true;
    }
    
    public boolean addAll(Collection<? extends Character> chars) {
        str = str.append(charCollectionToString(chars));
        return true;
    }
    
    public void clear() {
        str.setLength(0);
    }
    
    public boolean contains(Object object) {
        return str.indexOf(object.toString()) != -1;
    }
    
    public boolean containsAll(Collection<?> chars) {
        for(Object o : chars) {
            if(!contains(o))
                return false;
        }
        return true;
    }
    
    public Character get(int i) {
        return str.charAt(i);
    }
    
    public int indexOf(Object object) {
        return str.indexOf(object.toString());
    }
    
    public boolean isEmpty() {
        return str.length() == 0;
    }
    
    public Iterator<Character> iterator() {
        return new StringIterator();
    }
    
    public int lastIndexOf(Object object) {
        return str.lastIndexOf(object.toString());
    }
    
    public ListIterator<Character> listIterator() {
        return new StringIterator();
    }
    
    
    public ListIterator<Character> listIterator(int i) {
        StringIterator si = new StringIterator();
        si.i = i-1;
        return si;
    }
    
    public Character remove(int i) {
        char rval = str.charAt(i);
        str.deleteCharAt(i);
        return rval;
    }
    
    public boolean remove(Object object) {
        int i = str.indexOf(object.toString());
        if(i == -1)
            return false;
        else {
            str.deleteCharAt(str.indexOf(object.toString()));
            return true;
        }
    }
    
    public boolean removeAll(Collection<?> chars) {
        boolean rval = true;
        for(Object o : chars) {
            rval = remove(o) && rval;
        }
        return rval;
    }
    
    public boolean retainAll(Collection<?> chars) {
        boolean rval = false;
        for(int i = 0; i < str.length(); i++) {
            if(!chars.contains(str.charAt(i))) {
                str.deleteCharAt(i--);
                rval = true;
            }
        }
        return rval;
        
    }
    
    public Character set(int i, Character c) {
        char c2 = str.charAt(i);
        str.setCharAt(i,c);
        return c2;
    }
    
    public int size() {
        return str.length();
    }
    
    public List<Character> subList(int i0, int i1) {
        return new StringList(str.subSequence(i0,i1));
    }
    
    public Object[] toArray() {
        char[] charArray = str.toString().toCharArray();
        Character[] rval = new Character[charArray.length];
        for(int i = 0; i < charArray.length; i++) {
            rval[i] = charArray[i];
        }
        return rval;
    }
    
    public <T> T[] toArray(T[] t) {
        throw new UnsupportedOperationException();
    }
    
    public Appendable append(char c) throws IOException {
        return str.append(c);
    }
    
    public Appendable append(CharSequence charSequence) throws IOException {
        return str.append(charSequence);
    }
    
    public Appendable append(CharSequence charSequence, int i, int i0) throws IOException {
        return str.append(charSequence,i,i0);
    }
    
    public char charAt(int i) {
        return str.charAt(i);
    }
    
    public int compareTo(CharSequence cs) {
        int i = 0;
        for(; i < cs.length() && i < str.length(); i++) {
            char c1 = str.charAt(i);
            char c2 = cs.charAt(i);
            if(c1 != c2) {
                if(c1 < c2)
                    return -1;
                else
                    return 1;
            }
        }
        if(str.length() < cs.length()) {
            return -1;
        } else if(str.length() > cs.length()) {
            return +1;
        } else {
            return 0;
        }
    }
    
    public int length() {
        return str.length();
    }
    
    public CharSequence subSequence(int i, int i0) {
        return str.subSequence(i,i0);
    }
    
    public String toString() {
        return str.toString();
    }
    
    class StringIterator implements ListIterator<Character> {
        private int i = -1;
        
        public boolean hasNext() { return i < str.length() - 1; }
        public boolean hasPrevious() { return i > 0; }
        public Character next() { return str.charAt(++i); }
        public Character previous() { return str.charAt(--i); }
        public int nextIndex() { return i + 1; }
        public int previousIndex() { return i - 1; }
        public void add(Character c) {
            str = str.insert(i+1,c);
        }
        public void set(Character c) {
            str.setCharAt(i,c);
        }
        public void remove() {
            str.deleteCharAt(i);
        }
    }
}
