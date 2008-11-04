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
package srl.corpus;

import srl.corpus.token.*;

/**
 * Identical to a Lucene token.
 */
public class SrlToken extends org.apache.lucene.analysis.Token {
    
    protected SrlToken(String s, int begin, int end) { super(s,begin,end); }
    
    public static SrlToken makeToken(String image, int kind, int begin, int end) {
	if(kind == StandardTokenizerConstants.BEGIN_TAG) {
	    return new BeginTagToken(image, begin, end);
	} else if(kind == StandardTokenizerConstants.END_TAG) {
	    return new EndTagToken(image, begin, end);
	} else {
	    return new SrlToken(image, begin, end);
	}
    }
}