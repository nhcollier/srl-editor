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
public class PartialLiteral implements TypeExpr {
    public final String partLiteral;
    public final int part;
    private TypeExpr next;
    public static final int BEGIN = 0;
    public static final int END = 1;
    public static final int CONTAINS = 2;

    public PartialLiteral(String partLiteral, int part) {
        this.partLiteral = partLiteral;
        this.part = part;
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
        if(part == BEGIN && token.termText().substring(0, partLiteral.length()).equals(partLiteral) ||
                part == END && token.termText().substring(token.termText().length()-partLiteral.length()-1).equals(partLiteral) ||
                part == CONTAINS && token.termText().contains(partLiteral))
            return next;
        else
            return null;
    }

    public boolean canEnd() {
        return false;
    }

    public TypeExpr copy() {
        return new PartialLiteral(partLiteral, part);
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
}
