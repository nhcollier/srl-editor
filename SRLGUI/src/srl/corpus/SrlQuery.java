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
package srl.corpus;

import java.util.HashSet;
import java.util.Set;
import mccrae.tools.struct.Pair;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class SrlQuery {
    public StringBuffer query;
    public Set<String> wordLists;
    public Set<Pair<String,String>> entities;
    
    public SrlQuery() {
        wordLists = new HashSet<String>();
        query = new StringBuffer();
	entities = new HashSet<Pair<String,String>>();
    }
}
