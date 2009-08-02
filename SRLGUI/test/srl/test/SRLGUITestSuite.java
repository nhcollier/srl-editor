package srl.test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import srl.corpus.CorpusTest;
import srl.project.SrlProject;
import srl.project.SrlProjectTest;
import srl.rule.RuleTest;
import srl.wordlist.WordListEntryTest;
import srl.wordlist.WordListSetTest;

/**
 *
 * @author john
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({SrlProjectTest.class,CorpusTest.class,WordListEntryTest.class,WordListSetTest.class,RuleTest.class})
public class SRLGUITestSuite {

    public static SrlProject proj;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}