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

import java.io.StringReader;
import java.util.*;


// A dummy state to denote the end of the rule
import mccrae.tools.strings.Strings;
import srl.corpus.SrlDocument;
import srl.rule.parser.ParseException;
import srl.rule.parser.SrlParser;
import mccrae.tools.struct.*;
import org.apache.lucene.analysis.Token;
import srl.corpus.BeginTagToken;
import srl.corpus.EndTagToken;
import srl.corpus.SrlQuery;

/**
 *
 * @author john
 */
public class Rule implements Expr {

    public ListenableList<Head> heads;
    public ListenableList<TypeExpr> body;
    static TypeExpr successState;
    public String comment = "";
    public final int ruleType;
    public static int ENTITY_RULE = 0;
    public static int TEMPLATE_RULE = 1;
    

    static {
        successState = new SuccessState();
    }

    /**
     * Create a new rule
     */
    public Rule(int ruleType) {
        body = new ListenableList<TypeExpr>(new LinkedList<TypeExpr>());
        heads = new ListenableList<Head>(new LinkedList<Head>());
        this.ruleType = ruleType;
        if (ruleType != ENTITY_RULE && ruleType != TEMPLATE_RULE) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Create a new rule, defined by a string
     */
    public static Rule ruleFromString(String s, int ruleType)
            throws ParseException {
        SrlParser parser = new SrlParser(new StringReader(s));
        if (ruleType == ENTITY_RULE) {
            return parser.readNERule();
        } else if (ruleType == TEMPLATE_RULE) {
            return parser.readTRRule();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get a query suitable for finding potentially matching strings in a corpus
     * @return The lucene query
     */
    public SrlQuery getCorpusQuery() {
        SrlQuery query = new SrlQuery();
        for (TypeExpr te : body) {
            te.getQuery(query);
        }
        return query;
    }

    /**
     * Check if a sentence as a sequence of tokens
     * @param sentence A tokenized string
     * @return True if there is at least one match in the string
     */
    public boolean matches(SrlDocument sentence) {
        return !getMatch(sentence, true).isEmpty();
    }

    /**
     * Find the matches
     * @param sentence A tokenizer
     * @param firstOnly Only look for the first match
     * @return The list of matches
     */
    public List<HashMap<Entity, SrlMatchRegion>> getMatch(SrlDocument sentence, boolean firstOnly) {
        TypeExpr typeExpr;
        ListIterator<Token> iter1 = sentence.listIterator();
        LinkedList<HashMap<Entity, SrlMatchRegion>> rval = new LinkedList<HashMap<Entity, SrlMatchRegion>>();
        int i = 0;
        MAIN: while (iter1.hasNext()) {
            Token tk = iter1.next();
            if (!(tk instanceof BeginTagToken) && !(tk instanceof EndTagToken) && tk.termText().matches("\\s*")) {
                continue;
            } else if (tk instanceof BeginTagToken || tk instanceof EndTagToken) {
                i--;
            }
            if(body.isEmpty())
                return rval;
            typeExpr = body.get(0);
            resetSearch();
            if ((typeExpr = typeExpr.matches(tk, i++)) != null) {
                if (typeExpr == successState) {
                    onMatch(rval);
                    if (firstOnly) {
                        return rval;
                    }
                    continue;
                }
                int j = i;
                Iterator<Token> iter2 = sentence.listIterator(iter1.nextIndex());
                while (iter2.hasNext()) {
                    tk = iter2.next();
                    // Skip whitespace tokens
                    if (!(tk instanceof BeginTagToken) && !(tk instanceof EndTagToken) && tk.termText().matches("\\s*")) {
                        j++;
                        continue;
                    } else if (tk instanceof BeginTagToken || tk instanceof EndTagToken) {
                        j--;
                    }
                    if (typeExpr == null) {
                        break;
                    } // Match failed

                    typeExpr = typeExpr.matches(tk, j++);
                    if (typeExpr == successState) {
                        onMatch(rval);
                        if (firstOnly) {
                            return rval;
                        }
                        continue MAIN;
                    }
                }
                if (typeExpr != null && typeExpr.canEnd()) {
                    if(typeExpr instanceof Entity) {
                       ((Entity)typeExpr).match.endRegion = j+1;
                    }   
                    onMatch(rval);
                    if (firstOnly) {
                        return rval;
                    }
                }
            }
        }
        resetSearch();
        return rval;
    }

    private void onMatch(LinkedList<HashMap<Entity, SrlMatchRegion>> rval) {
        HashMap<Entity, SrlMatchRegion> match = new HashMap<Entity, SrlMatchRegion>();
        
        for (TypeExpr te : body) {
            if (te instanceof Entity) {
                match.put((Entity) te, ((Entity) te).match);
            }
        }
        rval.add(match);
        resetSearch();
    }

    private void resetSearch() {
        for (TypeExpr te : body) {
            te.reset();
        }
    }

    public List<String> getHeads(SrlDocument sentence) {
        List<String> rv = new LinkedList<String>();
        for (Head head : heads) {
            List<HashMap<Entity, SrlMatchRegion>> ents = getMatch(sentence, false);
            for (Map<Entity, SrlMatchRegion> map : ents) {
                for (Entity e : map.keySet()) {
                    if (e.var.equals(head.var)) {
                        // MULTIPLE MATCHES
                        rv.add(head.name + "(\"" + map.get(e).value.toString() + "\")");
                    }
                }
            }
        }
        return rv;
    }

    public void addHead(String clasz,
            String var) {
        heads.add(new Head(clasz, var));
    }

    public void addTypeExpr(TypeExpr typeExpr) {
        if (!body.isEmpty()) {
            body.get(body.size() - 1).setNext(typeExpr);
        }
        typeExpr.setNext(successState);
        body.add(typeExpr);
    }

    @Override
    public String toString() {
        return Strings.join(";", heads) + " :- " + Strings.join(" ", body);
    }

    public int getRuleType() {
        return ruleType;
    }
}

class SuccessState implements TypeExpr {

    public void getQuery(SrlQuery query) {
        throw new IllegalStateException();
    }

    public TypeExpr matches(Token token, int no) {
        return this;
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
}

