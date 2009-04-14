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
import srl.corpus.SrlQuery;
import org.apache.lucene.analysis.Token;
/**
 *
 * @author John McCrae, National Institute of Informatics
 */
    
class DummyNode implements TypeExpr {
    TypeExpr next;
    final String id;
    
    public DummyNode(String id) { 
        this.id = id;
    }
    
    public void getQuery(SrlQuery query) {
        throw new IllegalStateException();
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
        TypeExpr te = next.matches(token,no,stack);
        if(te == null) 
            next.reset();
        return te;
    }

    public void reset() {
    }

    public void setNext(TypeExpr te) {
        throw new IllegalStateException();
    }

    public void skip(Token token) {
    }

    public boolean canEnd() {
        return true;
    }

    public TypeExpr copy() {
        return new EntitySuccessState(next);
    }
}