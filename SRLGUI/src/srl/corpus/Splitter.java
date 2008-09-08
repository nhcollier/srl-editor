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
import org.apache.lucene.analysis.*;
import java.util.*;

/**
 *
 * @author john
 */
public interface Splitter {

    public List<SrlDocument> split(Collection<Token> doc, String docName);
}
