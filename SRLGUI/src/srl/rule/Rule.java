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
        Stack<MatchFork> stack = new Stack<MatchFork>();
        int i = -1;
        Token tk = null;
        MAIN: while (iter1.hasNext()) {
            while(!stack.empty() && stack.peek().used) {
                stack.pop();
            }
            // If the stack is not empty, keep looping until it is
            if(stack.empty()) {
                // Read next token
                tk = iter1.next();
                i++;
            }
            // Ignore empty tokens, do not count tag tokens
            if (!(tk instanceof BeginTagToken) && !(tk instanceof EndTagToken) && tk.termText().matches("\\s*")) {
                continue;
            } else if ((tk instanceof BeginTagToken || tk instanceof EndTagToken) && stack.empty()) {
                i--;
            }
            // Reset search
            if(body.isEmpty())
                return rval;
            typeExpr = body.get(0);
            resetSearch();
            // Match first token
            if ((typeExpr = typeExpr.matches(tk, i, stack)) != null) {
                // Check for single token match
                if (typeExpr == successState) {
                    onMatch(rval);
                    if (firstOnly) {
                        return rval;
                    }
                    continue;
                }
                // Otherwise carry on matching
                int j = i+1;
                Iterator<Token> iter2 = sentence.listIterator(iter1.nextIndex());
                while (iter2.hasNext()) {
                    Token tk2 = iter2.next();
                    // Skip whitespace tokens
                    if (!(tk2 instanceof BeginTagToken) && !(tk2 instanceof EndTagToken) && tk2.termText().matches("\\s*")) {
                        j++;
                        continue;
                    } else if (tk2 instanceof BeginTagToken || tk2 instanceof EndTagToken) {
                        j--;
                    }
                    if (typeExpr == null) {
                        break;
                    } // Match failed

                    // Check next token
                    typeExpr = typeExpr.matches(tk2, j++,stack);
                    if (typeExpr == successState) {
                        onMatch(rval);
                        if (firstOnly) {
                            return rval;
                        }
                        continue MAIN;
                    }
                }
                // Check to see if we are caught in "words(1,) <EOL>" trap
                if (typeExpr != null && typeExpr.canEnd()) {
                    if(typeExpr instanceof Entity) {
                       ((Entity)typeExpr).match.endRegion = j;
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
                ((Entity)te).match.sourceRule = this;
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
        List<HashMap<Entity, SrlMatchRegion>> ents = getMatch(sentence, false);
        Vector<Pair<Entity,SrlMatchRegion>> matches = srl.corpus.Corpus.sortMatches(ents);
        for (Pair<Entity,SrlMatchRegion> map : matches) {
            for (Head head : heads) {
                if (map.first.var.equals(head.var)) {
                    // MULTIPLE MATCHES
                    rv.add(head.name + "(\"" + map.second.value.toString() + "\")");
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

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
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

