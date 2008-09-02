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
package srl.wordlist;

import java.util.*;
import java.io.*;
import java.util.regex.*;
import mccrae.tools.strings.Strings;
import mccrae.tools.struct.*;
import org.apache.lucene.analysis.*;
import srl.corpus.Processor;

/**
 *
 * @author john
 */
public class WordList {
    public ListenableMap<String,ListenableSet<Entry>> wordLists;
    public final String name;
    private final Processor processor;
    public Map<String,String> comment = new HashMap<String,String>();
    
    public WordList(String name, Processor processor) {
        this.name = name;
        this.processor = processor;
        wordLists = new ListenableMap<String,ListenableSet<Entry>>(new HashMap<String,ListenableSet<Entry>>());
    }
    
    public static WordList loadFromFile(File file, Processor processor) throws IOException {
        System.out.println("Loading: " + file);
        String wlName = file.getName();
        if(wlName.matches(".*\\.wordlist\\.srl")) {
            wlName = wlName.substring(0,wlName.length()-13);
        }
        WordList wl = new WordList(wlName, processor);
        String cmt = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        for(String in = br.readLine(); in != null; in = br.readLine()) {
            String[] ss = in.split("#");
            in = ss[0];
            if(ss.length > 1 && ss[1].length() > 0) {
                ss[1] = ss[1].replaceAll("^\\s+", "");
                cmt = cmt + ss[1] + "\n";
            } 
            if(in.matches("\\s*"))
                continue;
            Matcher m = Pattern.compile("\\s*@(\\w+)\\((.*)\\)\\s*").matcher(in);
            if(!m.matches())
                throw new RuntimeException("Syntax Error at " + in);
            String listName = m.group(1);
            if(m.matches()) {
                ListenableSet<Entry> set = new ListenableSet<Entry>(new TreeSet<Entry>());
                char[] list = m.group(2).toCharArray();
                StringBuffer name = new StringBuffer();
                boolean inLiteral = false;
                for(int i = 0; i < list.length; i++) {
                    if(inLiteral) {
                        if(list[i] == '\\') {
                            name.append(list[i]);
                            name.append(list[++i]);
                        } else if(list[i] == '\"') {
                            set.add(wl.getEntry(name.toString()));
                            name = new StringBuffer();
                            inLiteral = false;
                        } else {
                            name.append(list[i]);
                        }
                    } else {
                        if(list[i] == '\"') {
                            inLiteral = true;
                        }
                    }
                }
                
                wl.wordLists.put(listName, set);
                wl.comment.put(listName, cmt);
                cmt = "";
            } else {
                System.err.println(in);
            }
        }
        
        allWordLists.putAll(wl.wordLists);
        for(String s : wl.wordLists.keySet()) {
            allWordSets.put(s, wl);
        }
        return wl;
    }
   
    
    public void write(File file) throws IOException {
        PrintStream ps = new PrintStream(file,"UTF-8");
        for(Map.Entry<String,ListenableSet<Entry>> entry : wordLists.entrySet()) {
            String cmt;
            if(comment.get(entry.getKey()) != null && 
                    comment.get(entry.getKey()).length() > 0) {
                cmt = "# " + comment.get(entry.getKey());
                cmt = cmt.replaceAll("(\n|\r)(?=.)", "\n# ");
                if(cmt.charAt(cmt.length() - 1) != '\n') {
                    cmt = cmt + "\n";
                }
            } else {
                cmt = "";
            }
            ps.println(cmt + "@" + entry.getKey() + "(\"" +
                Strings.join("\",\"", entry.getValue()) + "\")");
        }
        ps.close();
    }
    
    /**
     * Add a new list to this set of word lists
     * @param name The identifier for this name
     * @return True if the list was successfully added
     */
    public boolean addList(String name) {
        if(allWordLists.get(name) != null) 
            return false;
        ListenableSet<Entry> set = new ListenableSet<Entry>(new TreeSet<Entry>());
        wordLists.put(name, set);
        allWordLists.put(name, set);
        allWordSets.put(name, this);
        return true;
    }
    
    static Map<String,ListenableSet<Entry>> allWordLists = new HashMap<String,ListenableSet<Entry>>();
    static Map<String,WordList> allWordSets = new HashMap<String,WordList>();
    
    public static ListenableSet<Entry> getWordList(String wordListName) {
        return allWordLists.get(wordListName);
    }
    
    public static Set<String> getAllWordListNames() {
        return allWordLists.keySet();
    }
    
    public static WordList getWordListSet(String wordListName) {
        return allWordSets.get(wordListName);
    }
    
    public static SortedSet<WordList.Entry> getMatchSet(String name, String token) {
        SortedSet<WordList.Entry> set = ((SortedSet<WordList.Entry>)allWordLists.get(name).getSet());
        WordList wl = getWordListSet(name);
        LinkedList<String> l1 = new LinkedList<String>();
        LinkedList<String> l2 = new LinkedList<String>();
        l1.add(token);
        l2.add(token);
        l2.add("\uffff");
        return set.subSet(wl.getEntry(l1), wl.getEntry(l2));
    }
    
    public static void reset() {
        allWordLists = new HashMap<String,ListenableSet<Entry>>();
        allWordSets = new HashMap<String,WordList>();
    }
    
    public Entry getEntry(String s) {
        return new Entry(s);
    }
    
    private Entry getEntry(List<String> s) {
        return new Entry(s);
    }
    
    public class Entry implements Comparable<Entry> {
        List<String> words;
        String originalVal;
        
        public Entry(String val) {
            words = new LinkedList<String>();
            originalVal = val;
            TokenStream ts = processor.getTokenStream(val.toLowerCase());
            try {
                for(Token s = ts.next(); s != null; s = ts.next()) {
                    words.add(s.termText());
                }
            } catch(IOException x) {
                x.printStackTrace();
                throw new RuntimeException();
            }
        }
        
        Entry(List<String> words) {
            this.words = words;
            this.originalVal = "";
        }
        
        /** (expert) This function is used for constructing an Entry step by step
         * @param s The next token
         */
        public void addWord(String s) {
            words.add(s.toLowerCase());
        }
        
        public boolean matchable(Entry e) {
            if(e.words.size() > words.size())
                return false;
            for(int i = 0; i < e.words.size(); i++) {
                if(!e.words.get(i).equals(words.get(i)))
                    return false;
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Entry)
                return compareTo((Entry)obj) == 0;
            else
                return false;
        }

        public int compareTo(WordList.Entry o) {
            int n = Math.min(words.size(), o.words.size());
            for(int i = 0; i < n; i++) {
                int t = words.get(i).compareTo(o.words.get(i));
                if(t != 0)
                    return t;
            }
            if(words.size() < o.words.size()) {
                return -1;
            } else if(words.size() > o.words.size()) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return originalVal;
        }
    }
}
