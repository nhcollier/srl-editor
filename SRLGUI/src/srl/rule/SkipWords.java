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

import srl.corpus.SrlQuery;
import org.apache.lucene.analysis.Token;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class SkipWords implements TypeExpr {

    final int min,max;
    int i;
    TypeExpr next;
            
    public SkipWords(int min, int max) {
        this.min = min;
        this.max = max;
    }
    
    public void getQuery(SrlQuery query) {
    }

    public TypeExpr matches(Token token, int no) {
	if(i < min) {
	    i++;
	    return this;
	} 
        TypeExpr te = next.matches(token,no);
        if(te != null) {
            // We have already matched to our next state, so we go straight on
            return te;
        } else if(i < max) {
            next.reset();
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
    }

    @Override
    public String toString() {
        return "words(" + (min == 0 ? "" : min) + "," + (min == Integer.MAX_VALUE ? "" : max) + ")";
    }
    
    public boolean canEnd() {
        if(i >= min)
            return next.canEnd();
        else
            return false;
    }
}