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
import srl.corpus.BeginTagToken;
import srl.corpus.EndTagToken;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class SkipWords implements TypeExpr {
    /** The min number of tokens to match */
    public int min;
    /** The max number of tokens to match */        
    public int max;
    int i;
    TypeExpr next;
    int tagDepth = 0;
            
    public SkipWords(int min, int max) {
        this.min = min;
        this.max = max;
    }
    
    public void getQuery(SrlQuery query) {
        query.query.append("\" \"");
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
        // Stack: Two options if next matches 1/ Return next.next 2/ Ignore, return this
        // If this number/expr pair is not on the stack do 1/ and add to stack
        // If this number/expr pair is on top of the stack do 2/ and mark as used
        // If this number/expr pair is on the stack and marked do 2/
        // If this number/expr pair is on the stack and not marked do 1/ (This will only happen if more than one 
        //  pair is added to the stack in a single run).
        if(token instanceof BeginTagToken) {
            tagDepth++;
        }
        if(token instanceof EndTagToken) {
            tagDepth--;
            if(tagDepth > 0)
                return this;
        }
	if(i < min) {
            //if(token instanceof BeginTagToken || token instanceof EndTagToken)
                i++;
	    return this;
	} 
        MatchFork mf = MatchFork.find(stack, no, this);
        if(mf != null && (mf.used == true || stack.peek() == mf)) {
            stack.peek().split(no, this);
            if(i < max) {
                //if(token instanceof BeginTagToken || token instanceof EndTagToken)
                    i++;
                return this;
            } else {
                return null;
            }
        }
        TypeExpr te = next.matches(token,no,stack);
        if(te != null) {
            if((stack.empty() || stack.peek().tokenNo < no) &&
                    !(te == Rule.successState) &&
                    !(next instanceof EndTag))
                stack.push(new MatchFork(no,this));
            // We have already matched to our next state, so we go straight on
            return te;
        } else if(i < max) {
            next.reset();
            //if(token instanceof BeginTagToken || token instanceof EndTagToken)
                i++;
            return this;
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
       i = 0;
       tagDepth = 0;
    }

    @Override
    public String toString() {
        return "words(" + (min == 0 ? "" : min) + "," + (max == Integer.MAX_VALUE ? "" : max) + ")";
    }
    
    public boolean canEnd() {
        if(i >= min)
            return next.canEnd();
        else
            return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof SkipWords) {
            SkipWords l = (SkipWords)obj;
            return min == l.min && max == l.max;
        }
        return false;
    }

    public TypeExpr copy() {
        return new SkipWords(min, max);
    }
}