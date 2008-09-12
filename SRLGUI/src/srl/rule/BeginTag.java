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
import srl.corpus.*;
import mccrae.tools.struct.*;
import java.util.*;

/**
 *
 * @author John McCrae
 */
public class BeginTag implements TypeExpr {
    final String entityType, entityValue;
    TypeExpr next;
    
    public BeginTag(String entityType, String entityValue) {
        this.entityType = entityType;
        this.entityValue = entityValue;
    } 
    
    
    public void getQuery(SrlQuery query) {
        query.entities.add(new Pair<String,String>(entityType,entityValue));
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
        if(token instanceof BeginTagToken) {
            BeginTagToken btt = (BeginTagToken)token;
            if(btt.type.equals(entityType) && btt.val.equals(entityValue))
                return next;
            else
                return null;
        } else {
            return null;
        }
    }

    public void skip(Token token) {
        
    }

    public void setNext(TypeExpr te) {
        this.next = te;
    }

    public void reset() {
        
    }

    public boolean canEnd() {
        return false;
    }           
}
