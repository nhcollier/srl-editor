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

import java.io.StringReader;
import java.lang.reflect.Constructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

/**
 * Wraps the language processor, for easy interaction. That is the tokenizer
 * splitter and search indexer analyzer (essentially the tokenizer plus a stop
 * words list)
 * 
 * @author John McCrae, National Institute of Informatics
 */
public class Processor {
    final String analyzer, tokenizer, splitter;
    
    /** The names of the processors packaged with Lucene */
    public static final String[] langs = { "English", 
      "Brazilian", 
      "Chinese", 
      "CJK", 
      "Czech", 
      "Dutch", 
      "French", 
      "German", 
      "Greek", 
      "Russian",
      "Thai",
      "Japanese",
      "Pre-tokenized"
    };
    
    static final String[] analyzers = {
        "org.apache.lucene.analysis.standard.StandardAnalyzer",
        "org.apache.lucene.analysis.br.BrazilianAnalyzer", 
        "org.apache.lucene.analysis.cn.ChineseAnalyzer", 
        "org.apache.lucene.analysis.cjk.CJKAnalyzer", 
        "org.apache.lucene.analysis.cz.CzechAnalyzer", 
        "org.apache.lucene.analysis.nl.DutchAnalyzer", 
        "org.apache.lucene.analysis.fr.FrenchAnalyzer", 
        "org.apache.lucene.analysis.de.GermanAnalyzer", 
        "org.apache.lucene.analysis.el.GreekAnalyzer",
        "org.apache.lucene.analysis.ru.RussianAnalyzer", 
        "org.apache.lucene.analysis.th.ThaiAnalyzer",
        "srl.corpus.jp.JapaneseAnalyzer",
        "org.apache.lucene.analysis.standard.StandardAnalyzer",
    };
    
    static final String[] tokenizers = {
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "org.apache.lucene.analysis.cn.ChineseTokenizer", 
        "org.apache.lucene.analysis.cjk.CJKTokenizer", 
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.token.StandardTokenizer",
        "srl.corpus.th.LexToTokenizer",
        "srl.corpus.jp.JapaneseTokenizer",
        "srl.corpus.pre.PreTokenizer"
    };
    
    static final String[] splitters = {
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.StandardSplitter",
        "srl.corpus.pre.PreSplitter"
    };
    
    /** Construct a processor using on the standard names
     * @param name A name from langs
     */
    public Processor(String name) {
        for(int i = 0; i < langs.length; i++) {
            if(name.equals(langs[i])) {
                analyzer = analyzers[i];
                tokenizer = tokenizers[i];
                splitter = splitters[i];
                return;
            }
        }
        throw new IllegalArgumentException("Language " + name + " not known");
    }
    
    /** Construct a custom processor
     * @param analyzer The name of the analyzer class (must extend org.apache.lucene.analysis.Analyzer)
     * @param tokenizer The name of the tokenizer class (must extend org.apache.lucene.analysis.Tokenizer)
     */
    public Processor(String analyzer, String tokenizer, String splitter) {
        this.analyzer = analyzer;
        this.tokenizer = tokenizer;
        this.splitter = splitter;
    }
    
    private Analyzer a;
    
    /** Get an instance of the analyzer */
    public Analyzer getAnalyzer() {
        if(a == null) {
            try {
                return a = (Analyzer)Class.forName(analyzer).newInstance();
            } catch(Exception x) {
                x.printStackTrace();
                return null;
            }
        } else {
            return a;
        }
    }
    
    /** 
     * Get a stream of tokens 
     * @param s The value to tokenize
     * @return The token stream
     */
    public TokenStream getTokenStream(String s) {
        try {
            Class[] params = { java.io.Reader.class };
            Constructor c = Class.forName(tokenizer).getConstructor(params);
            Object[] p = new Object[1];
            p[0] = new StringReader(s);
            return (TokenStream)c.newInstance(p);
        } catch(Exception x) {
            x.printStackTrace();
            return null;
        }
            
    }
    
    private Splitter s;
    
    /**
     * Get a sentence splitter
     * @return The sentence splitter
     */
    public Splitter getSplitter() {
        if(s == null) {
            try {
                return s = (Splitter)Class.forName(splitter).newInstance();
            } catch(Exception x) {
                x.printStackTrace();
                return null;
            }
        } else {
            return s;
        }
    }
    
    public String getAnalyzerName() { return analyzer; }
    public String getTokenizerName() { return tokenizer; }
    public String getSplitterName() { return splitter; }
}


