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

/** This represents a match by an SRL rule
 * @author john
 */
public class SrlMatchRegion {
    /** The index of the first matched token */
    public int beginRegion = -1;
    /** The index after the last matched token */
    public int endRegion = -1;
    /** The value of the string matched */
    public StringBuffer value = new StringBuffer();
    /** The rule, which matched this region */
    public Rule sourceRule;
    
    /** Clear this match region */
    public void reset() {
	beginRegion = endRegion = -1;
	value = new StringBuffer();
    }
    
    @Override
    public String toString() {
        return value + "[" + beginRegion + "," + endRegion + "]";
    }          
}