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
public class CaseSensitiveLiteral implements TypeExpr {

    final String literal;
    
    TypeExpr next;
            
    public CaseSensitiveLiteral(String literal) {
        this.literal = literal.substring(1, literal.length() - 1);
    }
    
    public void getQuery(SrlQuery query) {
        if(query.query.charAt(query.query.length()-1) != '\"')
            query.query.append(" ");
        query.query.append(literal.replaceAll("([\\\"\\\'])", "\\$1"));
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack, List<Token> lookBackStack) {
        if(token.termText().equals(literal)) {
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
        return "case(\"" + literal + "\")";
    }

    public boolean canEnd() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof CaseSensitiveLiteral) {
            return literal.equals(((CaseSensitiveLiteral)obj).literal);
        }
        return false;
    }

    public TypeExpr copy() {
        return new Literal(literal);
    }
}
