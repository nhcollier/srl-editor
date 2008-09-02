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

import srl.corpus.*;
import org.apache.lucene.analysis.*;

/**
 *
 * @author john
 */
public interface TypeExpr {
    /** Get a query in a format suitable for Lucene */
    public void getQuery(SrlQuery query);
    /** Query if this TypeExpr matches the current token 
     * @param token A single token in the string
     * @param tokenNo The current token number
     * @return The next TypeExpr to be checked against the string of tokens, note this may be this object!
     */
    public TypeExpr matches(Token token, int tokenNo);
    /** At the end of a match reset all variables (eg skipwords number)
     */
    public void reset();
    /** Add the next typeExpr in this rule. NB do not call this but implement to add new TypeExprs
     * @param te The next object
     */
    public void setNext(TypeExpr te);
    /** Inform the TypeExpr of skipped tokens (i.e., whitespace)
     * @param s The skipped token;
     */
    public void skip(Token token);
    /** Is it possible to progress from here to the end of the automata
     */
    public boolean canEnd();
}
