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

import srl.corpus.SrlQuery;
import srl.wordlist.*;
import java.util.*;
import mccrae.tools.struct.Pair;
import org.apache.lucene.analysis.Token;

/**
 *
 * @author john
 */
public class StrMatchApprox implements TypeExpr {

    final String wordListName;
    final double matchAmount;
    
    TypeExpr next;
            
    public StrMatchApprox(String wordListName, double matchAmount) {
        this.wordListName = wordListName;
        this.matchAmount = matchAmount;
    }
    
    public void getQuery(SrlQuery query) {
        query.query.append("\" \"");
        query.wordLists.add(wordListName);
    }

    public TypeExpr matches(Token token, int no, Stack<MatchFork> stack) {
        Set<WordListEntry> list = WordList.getWordList(wordListName);
        for(WordListEntry wle : list) {
            String s = wle.toString();
            if(1.0 - (double)levenshteinDistance(token.termText().toCharArray(),s.toCharArray()) / 
	       (double)Math.max(s.length(), token.termText().length())
	       <= matchAmount) {
                return next;
            }
        }
        return null;
    }
    
    private static int levenshteinDistance(char[] s1, char[] s2) {
        int[] matrix = new int[s1.length * s2.length];
        int n = s1.length;
        for(int i = 0; i < s1.length; i++)
            matrix[i] = i;
        for(int i = 0; i < s2.length * n; i += n) {
            matrix[i] = i / n;
        }
        for(int i = 0; i < s1.length; i++) {
            for(int j = 0; j < s2.length; j++) {
                int cost = s1[i] == s2[j] ? 0 : 1;
                matrix[i + n * j] = Math.min(Math.min(matrix[i - 1 + n *j] + 1, matrix[i + n * (j-1)] + 1), matrix[i - 1 + n * (j-1)] + cost);
            }
        }
        return matrix[matrix.length-1];
    }

    public void setNext(TypeExpr te) {
        next = te;
    }

    public void skip(Token token) {
       
    }

    public void reset() {
       
    }

    @Override
    public String toString() {
        return "approx(@" + wordListName + "," + (matchAmount * 100) + "%)";
    }

    public boolean canEnd() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StrMatchApprox) {
            StrMatchApprox sma = (StrMatchApprox)obj;
            return sma.matchAmount == matchAmount && sma.wordListName.equals(wordListName);
        }
        return false;
    }

    public TypeExpr copy() {
        return new StrMatchApprox(wordListName, matchAmount);
    }
}
