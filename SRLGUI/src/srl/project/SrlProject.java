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
package srl.project;

import srl.rule.*;
import srl.wordlist.*;
import srl.corpus.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import mccrae.tools.strings.Strings;
import mccrae.tools.struct.ListenableList;
import mccrae.tools.struct.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import srl.rule.parser.ParseException;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class SrlProject {
    public List<RuleSet> entityRulesets;
    public List<RuleSet> templateRulesets;
    public ListenableList<WordList> wordlists;
    public final StringBuffer name = new StringBuffer();
    public final StringBuffer description = new StringBuffer();
    public ListenableList<Pair<String,String>> entities = 
            new ListenableList<Pair<String, String>>(new LinkedList<Pair<String, String>>());
    public Corpus corpus;
    File path;
    boolean modified;

    /**
     * Create a new SrlProject object with a new SrlProject
     * @param path The path where this is located
     * @param analyzer The analyzer used for this corpus
     * @throws java.lang.IllegalArgumentException If path exists and is not an empty directory
     * @throws IOException If a disk error occurred
     */
    public SrlProject(File path, Processor processor) throws IllegalArgumentException, IOException {
        if (path.exists()) {
            if (!path.isDirectory()) {
                throw new IllegalArgumentException(path.toString() + " is not a directory!");
            } else if (path.listFiles().length != 0) {
                throw new IllegalArgumentException(path.toString() + " is not empty!");
            }
        } else if (!path.mkdir()) {
            throw new IOException("Could not create directory " + path);
        }
        corpus = Corpus.openCorpus(new File(path, "corpus"), processor, true);
        entityRulesets = new LinkedList<RuleSet>();
        if (!(new File(path, "entity_rules")).mkdir()) {
            throw new IOException("Could not create directory " + path.toString() + "entity_rules");
        }
        templateRulesets = new LinkedList<RuleSet>();
        if (!(new File(path, "template_rules")).mkdir()) {
            throw new IOException("Could not create directory " + path.toString() + "template_rules");
        }
        WordList.reset();
        wordlists = new ListenableList<WordList>(new LinkedList<WordList>());
        if (!(new File(path, "wordlists")).mkdir()) {
            throw new IOException("Could not create directory " + path.toString() + "wordlists");
        }
        modified = true;
        this.path = path;
    }

    private SrlProject() {
        entityRulesets = new LinkedList<RuleSet>();
        templateRulesets = new LinkedList<RuleSet>();
        wordlists = new ListenableList<WordList>(new LinkedList<WordList>());
    }

    public static SrlProject openSrlProject(File path) throws IllegalArgumentException, IOException, SAXException {
        SrlProject proj = new SrlProject();
        proj.path = path;
        WordList.reset();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        SrlProjectDocumentHandler handler = new SrlProjectDocumentHandler(proj);
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
        xr.parse(new InputSource(new FileInputStream(new File(path, "project.xml"))));
        proj.modified = false;
        return proj;
    }

    public void openCorpus(Processor processor) throws IOException {
        corpus = Corpus.openCorpus(new File(path, "corpus"), processor, false);
    }

    public void openWordList(String wordList) throws IOException {
        wordlists.add(WordList.loadFromFile(new File(new File(path.getPath(), "wordlists"), wordList + ".wordlist.srl"), corpus.getProcessor()));
        modified = true;
    }

    public void openRuleSet(String ruleSet, int ruleType) throws IOException, ParseException {
        if(ruleType == Rule.ENTITY_RULE)
            entityRulesets.add(RuleSet.loadFromFile(new File(new File(path, "entity_rules"), ruleSet + ".rule.srl"),ruleType));
        else if(ruleType == Rule.TEMPLATE_RULE)
            templateRulesets.add(RuleSet.loadFromFile(new File(new File(path, "template_rules"), ruleSet + ".rule.srl"),ruleType));
        else
            throw new IllegalArgumentException();
        modified = true;
    }

    public void writeProject() throws IOException {
        for (WordList wl : wordlists) {
            File f = new File(new File(path, "wordlists"), wl.name + ".wordlist.srl");
            if (!f.exists()) {
                f.createNewFile();
            }
            wl.write(f);
        }
        for (RuleSet rs : entityRulesets) {
            File f = new File(new File(path, "entity_rules"), rs.name + ".rule.srl");
            if (!f.exists()) {
                f.createNewFile();
            }
            rs.write(f);
        }
        for (RuleSet rs : templateRulesets) {
            File f = new File(new File(path, "template_rules"), rs.name + ".rule.srl");
            if (!f.exists()) {
                f.createNewFile();
            }
            rs.write(f);
        }
        corpus.saveCorpus(new File(path, "corpus"));
        writeXML();
        modified = false;
    }

    private void writeXML() throws IOException {
        PrintStream ps = new PrintStream(new File(path, "project.xml"));
        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        ps.println("\t<!DOCTYPE srlproject [");
        ps.println("\t<!ELEMENT srlproject (description, corpus, entities, entity_rulesets, template_rulesets, wordlists)>");
        ps.println("\t<!ELEMENT description (#PCDATA)>");
        ps.println("\t<!ATTLIST srlproject name CDATA #REQUIRED>");
        ps.println("\t<!ELEMENT entity_rulesets (ruleset*)>");
        ps.println("\t<!ELEMENT template_rulesets (ruleset*)>");
        ps.println("\t<!ELEMENT wordlists (wordlist*)>");
        ps.println("\t<!ELEMENT corpus EMPTY>");
        ps.println("\t<!ATTLIST corpus analyzer CDATA #REQUIRED> ");
        ps.println("\t<!ATTLIST corpus tokenizer CDATA #REQUIRED>");
        ps.println("\t<!ATTLIST corpus splitter CDATA #REQUIRED>");
        ps.println("\t<!ELEMENT ruleset (#PCDATA)>");
        ps.println("\t<!ELEMENT wordlist (#PCDATA)>");
        ps.println("\t<!ELEMENT entities (entity*)>");
        ps.println("\t<!ELEMENT entity (type,val)>");
        ps.println("\t<!ELEMENT type (#PCDATA)>");
        ps.println("\t<!ELEMENT val (#PCDATA)>");
        ps.println("]>");
        ps.println("<srlproject name=\"" + Strings.chomp(name.toString()) + "\">");
        ps.println("<description>" + Strings.chomp(description.toString()) + "</description>");
        ps.println("\t<corpus analyzer=\"" + corpus.getProcessor().getAnalyzerName() + 
                "\" tokenizer=\"" + corpus.getProcessor().getTokenizerName() +
                "\" splitter=\"" + corpus.getProcessor().getSplitterName() + "\"/>");
        ps.println("\t<entities>");
        for(Pair<String,String> entity : entities) {
            ps.println("\t\t<entity>");
            ps.println("\t\t\t<type>" + entity.first + "</type>");
            ps.println("\t\t\t<val>" + entity.second + "</val>");
            ps.println("\t</entity>");
        }
        ps.println("\t</entities>");
        ps.println("\t<entity_rulesets>");
        for (RuleSet rs : entityRulesets) {
            ps.println("\t\t<ruleset>" + rs.name + "</ruleset>");
        }
        ps.println("\t</entity_rulesets>");
        ps.println("\t<template_rulesets>");
        for (RuleSet rs : templateRulesets) {
            ps.println("\t\t<ruleset>" + rs.name + "</ruleset>");
        }
        ps.println("\t</template_rulesets>");
        ps.println("\t<wordlists>");
        for (WordList wl : wordlists) {
            ps.println("\t\t<wordlist>" + wl.name + "</wordlist>");
        }
        ps.println("\t</wordlists>");
        ps.println("</srlproject>");
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified() {
        modified = true;
    }
}
