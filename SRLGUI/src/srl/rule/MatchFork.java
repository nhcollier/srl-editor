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

/**
 * @author John McCrae, National Institute of Informatics
 */
public class MatchFork {
    public int tokenNo;
    public TypeExpr typeExpr;
    public boolean used;
    
    public MatchFork(int tokenNo, TypeExpr typeExpr) {
        this.tokenNo = tokenNo;
        this.typeExpr = typeExpr;
        used = false;
    }
    
    public boolean split(int tokenNo, TypeExpr typeExpr) {
        if(this.tokenNo == tokenNo && this.typeExpr == typeExpr) {
            used = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MatchFork) {
            MatchFork mf = (MatchFork)obj;
            return tokenNo == mf.tokenNo && typeExpr == mf.typeExpr;
        } else
            return false;
    }
    
    
}
