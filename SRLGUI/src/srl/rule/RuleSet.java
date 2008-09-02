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
import java.io.*;
import srl.rule.parser.*;
import mccrae.tools.struct.*;

/**
 *
 * @author john
 */
public class RuleSet {

    public ListenableList<Pair<String,Rule>> rules;
    public String name;
    public int ruleType;
    
    public RuleSet(int ruleType, String name) {
        this.name = name;
        this.ruleType = ruleType;
        rules = new ListenableList<Pair<String,Rule>>(new LinkedList<Pair<String,Rule>>());
    }
    
    private RuleSet(int ruleType) {
        this.ruleType = ruleType;
        rules = new ListenableList<Pair<String,Rule>>(new LinkedList<Pair<String,Rule>>());
    }
    
    public static RuleSet loadFromFile(File patternFile, int ruleType) throws IOException, ParseException {
        System.out.println("Loading: " + patternFile);
        SrlParser parse = new SrlParser(new FileInputStream(patternFile),"UTF-8");
        RuleSet ps = new RuleSet(ruleType);
        if(ruleType == Rule.ENTITY_RULE)
            parse.readNERules(ps);
        else if(ruleType == Rule.TEMPLATE_RULE)
            parse.readTRRules(ps);
        else
            throw new IllegalArgumentException();
        ps.name = patternFile.getName();
        if(ps.name.matches(".*\\.rule\\.srl")) {
            ps.name = ps.name.substring(0,ps.name.length()-9);
        }
        return ps;
    }
    
    public void write(File file) throws IOException {
        PrintStream ps = new PrintStream(file,"UTF-8");
        for(Pair<String,Rule> r : rules) {
            String comment = r.second.comment;
            comment = comment.replaceAll("(\n|\r)", "\n# ");
            if(comment.matches(".*\n# "))
                comment = comment.substring(0,comment.length()-3);
            if(comment.length() > 0) 
                ps.println("# " + comment);
            
            ps.println(r.first + ": " + r.second.toString());
        }
        ps.close();
    }
    
}
