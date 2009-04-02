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
public class OptionalLiteral implements TypeExpr {
    public final String literal;
    private TypeExpr next;

    public OptionalLiteral(String literal) {
        this.literal = literal.toLowerCase().substring(1,literal.length()-1);
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
        if(token.termText().toLowerCase().equals(literal)) {
            return next;
        } else 
            return next.matches(token, tokenNo, stack);
    }

    public boolean canEnd() {
        return true;
    }

    public TypeExpr copy() {
        return new OptionalLiteral(literal);
    }

    public void getQuery(SrlQuery query) {
    }

    public void reset() {
    }

    public void setNext(TypeExpr te) {
        next = te;
    }

    public void skip(Token token) {
    }
    
    public String toString() {
        return "optional(\"" + literal + "\")";
    }
}
