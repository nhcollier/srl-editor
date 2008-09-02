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
package srl.rule;

import srl.wordlist.*;
import java.util.*;
import org.apache.lucene.analysis.Token;
import srl.corpus.SrlQuery;

/**
 *
 * @author john
 */
public class StrMatch implements TypeExpr {

    TypeExpr next;
    String wordListName;
    SortedSet<WordList.Entry> matches = null;
    WordList.Entry currentMatch = null;

    public StrMatch(String wordListName) {
        this.wordListName = wordListName;
    }

    public void getQuery(SrlQuery query) {
        query.wordLists.add(wordListName);
    }

    public TypeExpr matches(Token token, int no) {
        if(matches == null) {
            matches = WordList.getMatchSet(wordListName, token.termText().toLowerCase());
            currentMatch = WordList.getWordListSet(wordListName).getEntry(token.termText().toLowerCase());
        } else {
            currentMatch.addWord(token.termText());
        }
        Iterator<WordList.Entry> wleIter = matches.iterator();
        while(wleIter.hasNext()) {
            WordList.Entry wle = wleIter.next();
            if(wle.equals(currentMatch))
                return next;
            if(!wle.matchable(currentMatch))
                wleIter.remove();
        }
        if(matches.isEmpty())
            return null;
        else
            return this;
    }

    public void setNext(TypeExpr te) {
        next = te;
    }

    public void skip(Token token) {

    }

    public void reset() {
        matches = null;
        currentMatch = null;
    }

    @Override
    public String toString() {
        return "strmatches(@" + wordListName + ")";
    }

    public boolean canEnd() {
        return false;
    }
}
