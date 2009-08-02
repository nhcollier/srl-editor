/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package srl.rule;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import mccrae.tools.process.StopSignal;
import mccrae.tools.struct.Pair;
import org.apache.lucene.document.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import srl.corpus.Corpus;
import srl.corpus.CorpusConcurrencyException;
import static org.junit.Assert.*;
import srl.corpus.SrlDocument;
import srl.corpus.SrlQuery;
import srl.rule.parser.ParseException;
import srl.test.SRLGUITestSuite;

/**
 *
 * @author john
 */
public class RuleTest {

    String[] failRules = { "\"geneva\"",
        "name(X) :- name(entity,X) { \"geneva\" }", 
        ":- \"geneva\""
    };
    int[] failType = { Rule.ENTITY_RULE, Rule.ENTITY_RULE, Rule.TEMPLATE_RULE };

    String[] testRules  = { " :- \"geneva\"",
    " :- list(@test_list)",
    " :- ortho(\"1N\")",
    " :- \"production\" words(,2) list(@test_list)",
    " :- begins(\"gen\")",
    " :- ends(\"eva\")",
    " :- contains(\"nev\")",
    " :- regex(\"[A-Z][0-9][A-Z][0-9]\")",
    " :- \"said\" optional(\"from\") \"geneva\"",
    " :- not(\"from\") \"geneva\"",
    " :- case(\"The\")",
    " :- name(location,X) { list(%test) }",
    "name(X) :- name(entity,X) { \"test\" }",
    "name(X) :- name*(entity,X) {\"test\"}"
    };
    int[] ruleType = { Rule.ENTITY_RULE, Rule.ENTITY_RULE,Rule.ENTITY_RULE,
    Rule.ENTITY_RULE,Rule.ENTITY_RULE,Rule.ENTITY_RULE,Rule.ENTITY_RULE,
    Rule.ENTITY_RULE,Rule.ENTITY_RULE,Rule.ENTITY_RULE,Rule.ENTITY_RULE,
    Rule.ENTITY_RULE,Rule.TEMPLATE_RULE,Rule.TEMPLATE_RULE};
    SrlQuery[] query = new SrlQuery[14];
    int[] matchCount = { 3, 3, 2, 2, 3, 3, 3, 2, 2, 1, 1, 3, 1, 1 };


    public RuleTest() {
        for(int i = 0; i < 14; i++) {
            query[i] = new SrlQuery();
        }
        query[0].query.append("\"geneva\"");
        query[1].wordLists.add("test_list");
        query[1].query.append(" ");
        query[2].query.append(" ");
        query[3].query.append("\"production\"  ");
        query[3].wordLists.add("test_list");
        query[4].query.append(" ");
        query[5].query.append(" ");
        query[6].query.append(" ");
        query[7].query.append(" ");
        query[8].query.append("\"said\" \"geneva\"");
        query[9].query.append(" \"geneva\"");
        query[10].query.append("\"The\"");
        query[11].wordListSets.add("test");
        query[11].query.append(" ");
        query[12].query.append("\"test\"");
        query[12].entities.add(new Pair<String,String>("name","entity"));
        query[13].query.append(" \"test\" ");
        query[13].entities.add(new Pair<String,String>("name","entity"));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of ruleFromString method, of class Rule.
     */
    @Test
    public void testRuleFromString() throws Exception {
        System.out.println("ruleFromString");
        for(int i = 0; i < testRules.length; i++) {
            System.out.println(i + " ");
            Rule result = Rule.ruleFromString(testRules[i], ruleType[i]);
            assertEquals(testRules[i], result.toString());
        }
        for(int i = 0; i < failRules.length; i++) {
            try {
                Rule results = Rule.ruleFromString(failRules[i], failType[i]);
            } catch(Exception x) {
                continue;
            }
            System.err.println("Syntactically false rule accepted: " + failRules[i]);
            fail();
        }
    }

    /**
     * Test of getCorpusQuery method, of class Rule.
     */
    @Test
    public void testGetCorpusQuery() {
        System.out.println("getCorpusQuery");
        for(int i = 0; i < testRules.length; i++) {
            System.out.println(i + " ");
            try {
                Rule r  = Rule.ruleFromString(testRules[i], ruleType[i]);
                SrlQuery q = r.getCorpusQuery();
                assertEquals(query[i].entities,q.entities);
                assertEquals(query[i].query.toString(),q.query.toString());
                assertEquals(query[i].wordListSets, q.wordListSets);
                assertEquals(query[i].wordLists, q.wordLists);
            } catch(Exception x) {
                x.printStackTrace();
                fail();
            }
        }
    }

    /**
     * Test of getMatch method, of class Rule.
     */
    @Test
    public void testGetMatch() throws IOException, ParseException, CorpusConcurrencyException {
        System.out.println("getMatch");
        Corpus corpus = SRLGUITestSuite.proj.corpus;
        for(int i = 0; i < testRules.length; i++) {
            System.out.println(i + " ");
            final Rule r = Rule.ruleFromString(testRules[i],ruleType[i]);
            final int ruleTyp = ruleType[i];
            final List<HashMap<Entity, SrlMatchRegion>> result = new LinkedList<HashMap<Entity, SrlMatchRegion>>();
            corpus.query(r.getCorpusQuery(), new Corpus.QueryHit() {

                public void hit(Document d, StopSignal signal) {
                    result.addAll(r.getMatch(new SrlDocument(d, SRLGUITestSuite.proj.processor, ruleTyp == Rule.TEMPLATE_RULE), false));
                }
            });
            assertEquals(matchCount[i],result.size());
        }
    }
}