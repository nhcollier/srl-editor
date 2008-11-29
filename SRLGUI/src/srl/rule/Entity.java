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

import java.util.*;
import mccrae.tools.strings.Strings;
import srl.corpus.SrlQuery;
import org.apache.lucene.analysis.Token;
import srl.corpus.BeginTagToken;
import srl.corpus.EndTagToken;
import mccrae.tools.struct.*;


/**
 *
 * @author john
 */
public class Entity implements TypeExpr, Expr, Comparable<Entity> {

    public List<TypeExpr> body;
    
    public final String entityType, entityValue, var;
    
    TypeExpr current;
    TypeExpr next;
    SrlMatchRegion match;
    public final int ruleType;
    //public static final TypeExpr successState = new EntitySuccessState(Rule.successState);
    
    public Entity(String entityType, String entityValue, String var, int ruleType) {
        this.entityType = entityType;
        this.entityValue = entityValue;
        this.var = var;
	this.ruleType = ruleType;
        body = new LinkedList<TypeExpr>();
        if(ruleType == Rule.TEMPLATE_RULE) {
            EndTag endTag = new EndTag(entityType);
            BeginTag bt = new BeginTag(entityType, entityValue);
            bt.setNext(endTag);
            body.add(bt);
            body.add(endTag);
        } 
        match = new SrlMatchRegion();
    }
    
    
    public void getQuery(SrlQuery query) {
        for(TypeExpr te : body) {
            te.getQuery(query);
        }
    }

    public TypeExpr matches(Token token, int tokenNo, Stack<MatchFork> stack) {
	current = current.matches(token, tokenNo,stack);
        if(current == null)
            return null;
	if(match.beginRegion < 0) {
            if(token instanceof BeginTagToken)
                match.beginRegion = tokenNo + 1;
            else 
                match.beginRegion = tokenNo;
	}
	
        if(!body.contains(current) && current == next) {
            if(token.termText().length() >= 1)
                match.value.append(token.termText());
            if(token instanceof EndTagToken)
                match.endRegion = tokenNo;
            else
                match.endRegion = tokenNo+1;
            if(match.value.toString().matches(".* "))
                match.value.deleteCharAt(match.value.length()-1);
	    return current;
        } else if(!body.contains(current)) { // Change this if nesting occurs
            match.endRegion = tokenNo;
            return current;
        } else {
            if(token.termText().length() > 1)
                match.value.append(token.termText() + (current instanceof EndTag ? "" : " "));
        }
        return this;
    }

    public boolean canEnd() {
        if(ruleType == Rule.ENTITY_RULE)
            return current.canEnd();
        else // Actually current.canEnd() always returns false in this case
            return false;
    }
    
    public void setNext(TypeExpr te) {
        next = te;
        if(!body.isEmpty())
            body.get(body.size()-1).setNext(te);
    }

    public void reset() {
        current = body.get(0);
        match = new SrlMatchRegion();
        for(TypeExpr te : body)
            te.reset();
    }
    
    //public String[] getEntity() {
    //    return match.toArray(new String[match.size()]);
    //}

    public void addTypeExpr(TypeExpr typeExpr) {
        if(!body.isEmpty()) {
            if(ruleType == Rule.ENTITY_RULE)
                body.get(body.size()-1).setNext(typeExpr);
            else
                body.get(body.size()-2).setNext(typeExpr);
        } else {
            current = typeExpr;
        }
        if(ruleType == Rule.ENTITY_RULE) {
            typeExpr.setNext(next);
            body.add(typeExpr);
        } else {
            body.add(body.size()-1,typeExpr);
            typeExpr.setNext(body.get(body.size()-1));
        }
    }
    
    public void skip(Token token) {
        match.value.append(token.termText());
    }

    public int getRuleType() {
        return ruleType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Entity other = (Entity) obj;
        if (this.body != other.body && (this.body == null || !this.body.equals(other.body))) {
            return false;
        }
        if (this.entityType != other.entityType && (this.entityType == null || !this.entityType.equals(other.entityType))) {
            return false;
        }
        if (this.entityValue != other.entityValue && (this.entityValue == null || !this.entityValue.equals(other.entityValue))) {
            return false;
        }
        if (this.var != other.var && (this.var == null || !this.var.equals(other.var))) {
            return false;
        }
        return true;
    }

    public int compareTo(Entity o) {
        int rv = var.compareTo(o.var);
        if(rv != 0) return rv;
        rv = entityType.compareTo(o.entityType);
        if(rv != 0) return rv;
        rv = entityValue.compareTo(o.entityValue);
        return rv;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + (this.body != null ? this.body.hashCode() : 0);
        hash = 31 * hash + (this.entityType != null ? this.entityType.hashCode() : 0);
        hash = 31 * hash + (this.entityValue != null ? this.entityValue.hashCode() : 0);
        hash = 31 * hash + (this.var != null ? this.var.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        int skip = (ruleType == Rule.ENTITY_RULE ? 0 : 1); 
        if(body.size() == 1 + skip + skip && body.get(skip) instanceof SkipWords && (((SkipWords)body.get(skip)).max == 1 && ruleType == Rule.ENTITY_RULE ||
                ((SkipWords)body.get(skip)).max == Integer.MAX_VALUE && ruleType == Rule.TEMPLATE_RULE)) {
            return entityType + "(" + entityValue + "," + var + ")";
        } else {
            return entityType + "(" + entityValue + "," + var + ") { " + Strings.join(" ", body.subList(skip, body.size()-skip)) + " }";
        }
    }

    public List<TypeExpr> getBody() {
        return body;
    }

    /**
     * Copy this type expr. Note this implementation does not copy the body
     * @return
     */
    public TypeExpr copy() {
        return new Entity(entityType, entityValue, var, ruleType);
    }
}
class EntitySuccessState implements TypeExpr {
    TypeExpr next;
    
    public EntitySuccessState(TypeExpr te) { 
        next = te; 
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
