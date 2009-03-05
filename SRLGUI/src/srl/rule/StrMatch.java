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
import mccrae.tools.struct.ListenableSet;
import org.apache.lucene.analysis.Token;
import srl.corpus.SrlQuery;

/**
 *
 * @author john
 */
public class StrMatch implements TypeExpr {

    TypeExpr next;
    String wordListName;
    SortedSet<WordListEntry> matches = null;
    WordListEntry currentMatch = null;
    final boolean set;

    public StrMatch(String wordListName) {
        if(wordListName.charAt(0) == '@')
            set = false;
        else if(wordListName.charAt(0) == '%')
            set = true;
        else
            throw new IllegalArgumentException("Word list name must start with @ or %");
        this.wordListName = wordListName.substring(1);
    }

    public void getQuery(SrlQuery query) {
        query.query.append("\" \"");
        if(set)
            query.wordListSets.add(wordListName);
        else
            query.wordLists.add(wordListName);
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
        if(matches == null) {
            if(set) {
                matches = new TreeSet<WordListEntry>();
                WordListSet wls = WordListSet.getWordListSetByName(wordListName);
                if(wls == null) {
                        throw new IllegalArgumentException("Cannot find word list set %" + wordListName);
                }
                for(Map.Entry<String,ListenableSet<WordListEntry>> entry : wls.getWordListSets()) {
                    matches.addAll(WordListSet.getMatchSet(entry.getKey(), token.termText().toLowerCase()));
                }
                currentMatch = wls.getEntry(token.termText().toLowerCase());
            } else {
                matches = new TreeSet<WordListEntry>(WordListSet.getMatchSet(wordListName, token.termText().toLowerCase()));
                currentMatch = WordListSet.getWordListSetByList(wordListName).getEntry(token.termText().toLowerCase());
            }
        } else {
            currentMatch.addWord(token.termText());
        }
        MatchFork mf = MatchFork.find(stack, no, this);
        if(mf != null && (mf.used == true || stack.peek() == mf)) {
            stack.peek().split(no, this);
            return this;
        }
        Iterator<WordListEntry> wleIter = matches.iterator();
        while(wleIter.hasNext()) {
            WordListEntry wle = wleIter.next();
            if(wle.equals(currentMatch)) {
                if(matches.size() > 1 && (stack.empty() || stack.peek().tokenNo < no))
                    stack.push(new MatchFork(no,this));
                return next;
            }
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
        return "strmatches(" + (set ? "%" : "@") + wordListName + ")";
    }

    public boolean canEnd() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StrMatch) {
            return ((StrMatch)obj).wordListName.equals(wordListName) && ((StrMatch)obj).set == set;
        }
        return false;
    }

    public TypeExpr copy() {
        return new StrMatch((set ? "%" : "@") + wordListName);
    }
}
