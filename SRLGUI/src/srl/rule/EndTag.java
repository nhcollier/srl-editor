/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package srl.rule;

import org.apache.lucene.analysis.Token;
import srl.corpus.*;
import java.util.*;
import mccrae.tools.struct.*;


/**
 *
 * @author John McCrae
 */
public class EndTag implements TypeExpr {
    final String entityType;
    
    public EndTag(String entityType) {
        this.entityType = entityType;
    } 
    
    
    public void getQuery(SrlQuery query) {
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
        if(token instanceof EndTagToken) {
            EndTagToken ett = (EndTagToken)token;
            if(ett.type.equals(entityType))
                return Entity.successState;
            else
                return null;
        } else {
            return null;
        }
    }

    public void skip(Token token) {
        
    }

    public void setNext(TypeExpr te) {
        if(te != Entity.successState)
            throw new IllegalArgumentException();
    }

    public void reset() {
        
    }

    public boolean canEnd() {
        return false;
    }
}
