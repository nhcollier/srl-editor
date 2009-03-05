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
import java.io.*;
import java.util.*;
import mccrae.tools.struct.Pair;
import org.apache.lucene.analysis.Token;
import srl.corpus.BeginTagToken;
import srl.corpus.CorpusExtractor;
import srl.corpus.EndTagToken;
import srl.corpus.SrlDocument;
import srl.rule.Rule;
import srl.rule.RuleSet;
import gnu.getopt.Getopt;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class Run {

    public static void main(String[] args) {
        PrintStream out = System.out;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        SrlProject proj = null;
        boolean namedEntity = false;
        
        Getopt opt = new Getopt("applyrules", args, "i:o:p:n");
        int c;
        try {
            while((c = opt.getopt()) != -1) {
                switch(c) {
                    case 'i':
                        in = new BufferedReader(new FileReader(opt.getOptarg()));
                        break;
                    case 'o':
                        out = new PrintStream(new File(opt.getOptarg()));
                        break;
                    case 'p':
                        proj = SrlProject.openSrlProject(new File(opt.getOptarg()), false);
                        break;
                    case 'n':
                        namedEntity = true;
                }
            }
        } catch(Exception x) {
                x.printStackTrace();
                System.err.println("Could not initialize: " + x.getMessage());
                return;
        }
        if(proj == null) {
            System.out.println("Please specify project");
            return;
        }
        StringBuffer doc = new StringBuffer();
        String s;
        try {
            while((s = in.readLine()) != null) {
                doc.append(s);
            }
        
       
            List<SrlDocument> sents = proj.processor.getSplitter().split(
                new SrlDocument("test", doc.toString(), proj.processor),"doc");
            List<SrlDocument> tagged = CorpusExtractor.tagSentences(sents, proj.entityRulesets,proj.processor);
            if(namedEntity) {
                for(SrlDocument srlDoc : tagged) {
                    for(Token tk : srlDoc) {
                        if(tk instanceof EndTagToken) {
                            out.print(((EndTagToken)tk).getTag() + " ");
                        } else if(tk instanceof BeginTagToken) {
                            out.print(((BeginTagToken)tk).getTag() + " ");
                        } else {
                            out.print(tk.termText() + " ");
                        }
                    }
                    out.println("");
                }
                return;
            }
            for(SrlDocument srlDoc : tagged) {
                for(RuleSet rs : proj.templateRulesets) {
                    for(Pair<String,Rule> r : rs.rules) {
                        List<String> heads;
                        try {
                             heads = r.second.getHeads(srlDoc);
                        } catch(Exception x) {
                            System.err.println("Error with rule " + r.first);
                            x.printStackTrace();
                            continue;
                        }
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
