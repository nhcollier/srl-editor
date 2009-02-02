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
 * This represents a query on the Corpus object. Corpus can be queried on 3
 * indices, the raw text, matches to any word list and any tagged entities
 * @author John McCrae, National Institute of Informatics
 */
public class SrlQuery {
    public StringBuffer query;
    public Set<String> wordLists;
    public Set<String> wordListSets;
    public Set<Pair<String,String>> entities;
    
    public SrlQuery() {
        wordLists = new HashSet<String>();
        wordListSets = new HashSet<String>();
        query = new StringBuffer();
	entities = new HashSet<Pair<String,String>>();
    }
}
