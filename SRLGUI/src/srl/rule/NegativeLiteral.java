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

import java.util.Stack;
import org.apache.lucene.analysis.Token;
import srl.corpus.SrlQuery;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class NegativeLiteral implements TypeExpr {
    public final String literal;
    public final StrMatch listMatcher;
    private TypeExpr next;
    private DummyNode dummy;

    public NegativeLiteral(String literal) {
        if(literal.charAt(0) == '\"') {
            this.literal = literal.toLowerCase().substring(1,literal.length()-1);
            listMatcher = null;
        } else {
            this.literal = null;
            listMatcher = new StrMatch(literal);
            dummy = new DummyNode("negative "+literal);
            listMatcher.setNext(dummy);
        }
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
        if(literal != null) {
            if(token.termText().toLowerCase().equals(literal)) {
                return null;
            } else 
                return next.matches(token, tokenNo, stack);
        } else {
            TypeExpr te = listMatcher.matches(token,tokenNo,stack);
            if(te == null)
                return next;
            if(te == dummy)
                return null;
            if(te == listMatcher)
                return this;
            else
                throw new IllegalStateException();
        }
    }

    public boolean canEnd() {
        return true;
    }

    public TypeExpr copy() {
        if(literal != null)
            return new NegativeLiteral(literal);
        else
            return new NegativeLiteral(listMatcher.wordListName);
    }

    public void getQuery(SrlQuery query) {
    }

    public void reset() {
        if(listMatcher != null)
            listMatcher.reset();
    }

    public void setNext(TypeExpr te) {
        next = te;
    }

    public void skip(Token token) {
    }
    
    public String toString() {
        if(literal != null)
            return "not(\"" + literal + "\")";
        else
            return "not(@" + listMatcher.wordListName + ")";
    }
    
}
