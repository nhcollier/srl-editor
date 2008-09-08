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
import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;
import java.io.*;
import java.util.*;
import mccrae.tools.struct.Pair;
import org.apache.lucene.analysis.Token;
import srl.corpus.SrlDocument;
import srl.rule.Entity;
import srl.rule.Head;
import srl.rule.Rule;
import srl.rule.RuleSet;
import srl.rule.SrlMatchRegion;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class ApplyRules {

    public static void main(String[] args) {
        PrintStream out = System.out;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        SrlProject proj = null;
        boolean namedEntity = false;
        GetOpt opt = new GetOpt(args, "i:o:p:n");
        int c;
        try {
            while((c = opt.getNextOption()) != -1) {
                switch(c) {
                    case 'i':
                        in = new BufferedReader(new FileReader(opt.getOptionArg()));
                        break;
                    case 'o':
                        out = new PrintStream(new File(opt.getOptionArg()));
                        break;
                    case 'p':
                        proj = SrlProject.openSrlProject(new File(opt.getOptionArg()));
                        break;
                    case 'n':
                        namedEntity = true;
                }
            }
        } catch(Exception x) {
                x.printStackTrace();
                System.err.println("Could not initialize: " + x.getMessage());
        }
        if(proj == null) {
            System.out.println("Please specifiy project");
            return;
        }
        StringBuffer doc = new StringBuffer();
        String s;
        try {
            while((s = in.readLine()) != null) {
                doc.append(s);
            }
        
       
            List<Collection<Token>> sents = proj.corpus.getProcessor().getSplitter().split(
                new SrlDocument("test", doc.toString(), proj.corpus.getProcessor()));
            List<SrlDocument> tagged = proj.corpus.tagSentences(sents, proj.entityRulesets);
            if(namedEntity) {
                for(SrlDocument srlDoc : tagged) {
                    for(Token tk : srlDoc) {
                        out.print(tk.termText() + " ");
                    }
                    out.println("");
                }
                return;
            }
            for(RuleSet rs : proj.templateRulesets) {
                for(Pair<String,Rule> r : rs.rules) {
                    for(SrlDocument srlDoc : tagged) {
                        List<String> heads = r.second.getHeads(srlDoc);
                        for(String head : heads)
                            out.println(head);
                    }
                }
            }
        
        } catch(IOException x) {
            x.printStackTrace();
            return;
        }
    }
    
}
