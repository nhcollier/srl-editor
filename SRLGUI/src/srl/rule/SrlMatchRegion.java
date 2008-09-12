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

public class SrlMatchRegion {
        public int beginRegion = -1;
	public int endRegion = -1;
	public StringBuffer value = new StringBuffer();
        public Rule sourceRule;
        
        public void reset() {
                beginRegion = endRegion = -1;
                value = new StringBuffer();
        }

    @Override
    public String toString() {
        return value + "[" + beginRegion + "," + endRegion + "]";
    }
        
        
}