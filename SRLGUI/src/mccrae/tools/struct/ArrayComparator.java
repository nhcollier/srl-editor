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
 * A Comparator for arrays by alphabetic order
 * 
 * @author John McCrae
 */
public class ArrayComparator<E extends Comparable> implements Comparator<E[]> {

    public int compare(E[] o1, E[] o2) {
        if(o1.length != o2.length)
            throw new UnsupportedOperationException();
        for(int i = 0; i < o1.length; i++) {
            int j = o1[i].compareTo(o2[i]);
            if(j != 0)
                return j;
        }
        return 0;
    }
}
