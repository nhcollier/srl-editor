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
package srl.corpus.jp;

import java.io.*;
import java.net.URISyntaxException;
import org.apache.lucene.analysis.*;
import java.util.*;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class JapaneseTokenizer extends Tokenizer {

    Tokenizer preTokenizer;
    String currentSentence;
    Token currentToken;
    int offset;
    static HashMap<String, Boolean> rules;

    public JapaneseTokenizer(Reader r) throws IOException {
        if(rules == null) 
            readRules();
    }
    
    @Override
    public Token next() throws IOException {
        if (currentSentence == null) {
            currentToken = preTokenizer.next();
            currentSentence = preTokenizer.toString();
            offset = 0;
        }
        if (currentSentence == null) {
            return null;
        }
        int last = offset;
        offset++;
        for (int i = offset; i + 1 < currentSentence.length(); i++) {
            String s = currentSentence.substring(offset - 1, offset + 1);
            if (rules.containsKey(s)) {
                if (rules.get(s) == Boolean.TRUE) {
                    return new Token(s.substring(last, offset), currentToken.startOffset() + last, currentToken.startOffset() + offset);
                }
            } else {
                String s2 = getForm(s, 1, 2);
                if (rules.containsKey(s2)) {
                    if (rules.get(s2) == Boolean.TRUE) {
                        return new Token(s.substring(last, offset), currentToken.startOffset() + last, currentToken.startOffset() + offset);
                    }
                }
                s2 = getForm(s, 0, 1);
                if (rules.containsKey(s2)) {
                    if (rules.get(s2) == Boolean.TRUE) {
                        return new Token(s.substring(last, offset), currentToken.startOffset() + last, currentToken.startOffset() + offset);
                    }
                }
                s2 = getForm(s, 0, 2);
                if (rules.containsKey(s2)) {
                    if (rules.get(s2) == Boolean.TRUE) {
                        return new Token(s.substring(last, offset), currentToken.startOffset() + last, currentToken.startOffset() + offset);
                    }
                }
            }
        }
        Token t = new Token(currentSentence.substring(last, currentSentence.length()),
                currentToken.startOffset() + last,
                currentToken.startOffset() + currentSentence.length());
        currentToken = null;
        currentSentence = null;
        return t;
    }
    
    public static void readRules() throws IOException {
        try {
            rules = new HashMap<String,Boolean>();
            BufferedReader br = new BufferedReader(new FileReader(new File(JapaneseTokenizer.class.getResource("srl/corpus/jp/rules").toURI())));
            for(String line = br.readLine(); line != null; line = br.readLine()) {
                rules.put(line.substring(0,2), line.charAt(3) == '+' ? Boolean.TRUE : Boolean.FALSE);
            }
        } catch(URISyntaxException x) {
            throw new RuntimeException("Unexpected Error: " + x.getMessage());
        }
        
    }

    private static char getType(char c) {
        if (c >= '\u4300' && c <= '\u9fcf') {
            return 'j';
        } else if (c >= '\u3040' && c <= '\u309f') {
            return 'h';
        } else if (c >= '\u30a0' && c <= '\u30ff') {
            return 'k';
        } else {
            return 'o';
        }
    }

    private static String getForm(String s, int st, int end) {
        String rv = "";
        for (int i = st; i < end; i++) {
            if (s.charAt(i) < '\u00ff') {
                rv = rv + s.charAt(i);
            } else {
                rv = rv + getType(s.charAt(i));
            }
        }
        return rv;
    }

    @Override
    public void reset() throws IOException {
        preTokenizer.reset();
        currentToken = null;
        currentSentence = null;
    }

    @Override
    public void close() throws IOException {
        preTokenizer.close();
        currentToken = null;
        currentSentence = null;
    }
}
