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

import org.apache.lucene.analysis.Token;
import srl.corpus.SrlQuery;
import java.util.*;
import mccrae.tools.struct.*;

/**
 *
 * @author john
 */
public class Literal implements TypeExpr {

    final String literal;
    
    TypeExpr next;
            
    public Literal(String literal) {
        this.literal = literal;
    }
    
    public void getQuery(SrlQuery query) {
        query.query.append(" ");
        query.query.append(literal);
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
        if(token.termText().toLowerCase().equals(literal.toLowerCase())) {
            return next;
        } else {
            return null;
        }
    }

    public void setNext(TypeExpr te) {
        next = te;
    }

    public void skip(Token token) {
       
    }

    public void reset() {
       
    }
    
    public String getVal() {
        return literal;
    }

    @Override
    public String toString() {
        return "\"" + literal + "\"";
    }

    public boolean canEnd() {
        return false;
    }
    
}
