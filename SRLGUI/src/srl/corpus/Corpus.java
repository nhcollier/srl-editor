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

import java.io.*;
import java.util.*;
import java.util.regex.*;
import mccrae.tools.process.StopSignal;
import mccrae.tools.strings.Strings;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;
import srl.rule.*;
import srl.wordlist.*;
import mccrae.tools.struct.*;

/**
 * This class wraps Lucene to provide all the tools useful for
 * access to the corpus.
 * @author John McCrae, National Institute of Informatics
 */
public class Corpus {

    IndexWriter indexWriter;
    IndexSearcher indexSearcher;
    Processor processor;
    HashSet<String> docNames;
    CorpusSupport support;
    public boolean nestingAllowed = true,  overlappingAllowed = true;

    private Corpus() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeCorpus();
    }

    /**
     * Close the corpus
     * @throws IOException If a disk error occured 
     */
    public void closeCorpus() throws IOException {
        if (indexWriter != null) {
            indexWriter.optimize();
            indexWriter.close();
            indexWriter = null;
        }
        if (indexSearcher != null) {
            indexSearcher.close();
        }
    }

    /** Opens the corpus so that new documents can be added
     * @param indexFile The location of the indexFile
     * @param processor An instance of the processor used
     * @param newIndex If true any index on existing path will be removed
     */
    public static Corpus openCorpus(File indexFile, Processor processor, boolean newIndex) throws IOException {
        Corpus c = new Corpus();
        c.processor = processor;
        c.indexWriter = new IndexWriter(indexFile, processor.getAnalyzer(), newIndex);
        c.docNames = new HashSet<String>();
        if (!newIndex) {
            c.closeIndex();
            c.docNames.addAll(c.extractDocNames());
            c.reopenIndex();
        }
        if (newIndex) {
            c.newSupport();
        } else {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(indexFile, "support")));
            try {
                c.support = (CorpusSupport) ois.readObject();
            } catch (ClassNotFoundException x) {
                throw new IOException(x.getMessage());
            }
            ois.close();
        }
        return c;
    }

    // Can't instantiate from static context (see above function)
    private void newSupport() {
        support = new CorpusSupport();
    }

    /**
     * Save the corpus 
     * @param file The path to save the corpus to
     * @throws IOException If a disk error occurred
     */
    public void saveCorpus(File file) throws IOException {
        if (isIndexOpen()) {
            closeIndex();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(file, "support")));
        oos.writeObject(support);
        oos.close();
    }

    /** Add a new document to corpus
     * @param name The name of the document
     * @param contents The text of the new document
     * @throws IllegalStateException if {@link #openIndex(boolean)} has not been called
     * @throws IOException The document couldn't be added
     * @throws IllegalArgumentException If the document name already exits
     */
    public void addDoc(String name, String contents) throws IOException, IllegalStateException, IllegalArgumentException {
        if (indexWriter == null) {
            reopenIndex();
        }
        if (docNames.contains(name)) {
            throw new IllegalArgumentException(name + " already exists in corpus");
        }
        Document d = new Document();
        d.add(new Field("originalContents", contents, Field.Store.YES, Field.Index.TOKENIZED));
        d.add(new Field("name", name, Field.Store.YES, Field.Index.TOKENIZED));
        //support.addWordListInfo(name, wordListForDoc(contents.toLowerCase()));
        docNames.add(name);
        int i = 0;
        for(Collection<org.apache.lucene.analysis.Token> sentence : processor.getSplitter().split(new SrlDocument(name, contents, processor))) {
            StringBuffer sent = new StringBuffer();
            Iterator<org.apache.lucene.analysis.Token> tkIter = sentence.iterator();
            while(tkIter.hasNext()) {
                sent.append(tkIter.next().termText());
                if(tkIter.hasNext())
                    sent.append(" ");
            }
            Document d2 = new Document();
            d2.add(new Field("contents", sent.toString(), Field.Store.YES, Field.Index.TOKENIZED));
            d2.add(new Field("name",name + " " + i, Field.Store.YES, Field.Index.TOKENIZED));
            support.addWordListInfo(name + " " + i, wordListForDoc(sent.toString()));
            indexWriter.addDocument(d2);
            i++;
        }
        d.add(new Field("sentCount", i+"", Field.Store.YES, Field.Index.NO));  
        indexWriter.addDocument(d);
    }

    protected Set<Pair<String, String>> wordListForDoc(String contents) {
        Set<Pair<String, String>> rval = new HashSet<Pair<String, String>>();
        for (String name : WordList.getAllWordListNames()) {
            for (WordList.Entry term : WordList.getWordList(name)) {
                if (contents.contains(term.toString())) {
                    rval.add(new Pair(name, term.toString()));
                    break;
                }
            }
        }
        return rval;
    }
    Directory dir;

    /** Close the corpus, after which no more documents can be added. Also commits the corpus to disk */
    public void closeIndex() throws IOException {
        indexWriter.optimize();
        dir = indexWriter.getDirectory();
        indexWriter.close();

        indexSearcher = new IndexSearcher(dir);
        indexWriter = null;
    }

    /** Reopen the index to add new documents*/
    public void reopenIndex() throws IOException {
        indexSearcher.close();
        indexWriter = new IndexWriter(dir, processor.getAnalyzer(), false);
        indexSearcher = null;
    }

    private class SrlHitCollector extends HitCollector {

        QueryHit qh;
        StopSignal signal;

        public SrlHitCollector(QueryHit qh, StopSignal signal) {
            this.qh = qh;
            this.signal = signal;
        }

        @Override
        public void collect(int doc, float arg1) {
            try {
                qh.hit(indexSearcher.doc(doc), signal);
            } catch (CorruptIndexException x) {
                x.printStackTrace();
            } catch (IOException x) {
                x.printStackTrace();
            }
        }
    }

    public interface QueryHit {

        public void hit(Document d, StopSignal signal);
    }

    /**
     * Query the corpus
     * @param query The query normally returned from Rule.getQuery()
     * @param collector Every hit is passed to the hit(Document) method of the collector
     * @throws java.io.IOException There was an disk error with the corpus
     */
    public void query(SrlQuery query, QueryHit collector) throws IOException {
        query(query, collector, null);
    }

    /**
     * Query the corpus
     * @param query The query normally returned from Rule.getQuery()
     * @param collector Every hit is passed to the hit(Document) method of the collector
     * @param signal An optional stop signal to abandon the query
     * @throws java.io.IOException There was an disk error with the corpus
     */
    public void query(SrlQuery query, QueryHit collector, StopSignal signal) throws IOException {
        if (indexSearcher == null) {
            closeIndex();
        }
        if (query.query.toString().matches("\\s*")) {
            if (query.wordLists.isEmpty()) {
                if (query.entities.isEmpty()) {
                    // Empty queries match everything (!)
                    System.out.println("Empty Query! This may significantly affect performance");
                    for (int i = 0; i < indexSearcher.maxDoc(); i++) {
                        collector.hit(indexSearcher.doc(i), signal);
                        if (signal.isStopped()) {
                            return;
                        }
                    }
                    return;
                }
            } else {
                Iterator<String> wlIter = query.wordLists.iterator();
                Set<String> docs = support.wordListToDoc.get(wlIter.next());
                while (wlIter.hasNext()) {
                    docs.retainAll(support.wordListToDoc.get(wlIter.next()));
                }
                for (String docName : docs) {
                    collector.hit(getDoc(docName), signal);
                    if (signal.isStopped()) {
                        return;
                    }
                }
            }
        }
        try {
            QueryParser qp = new QueryParser("contents", processor.getAnalyzer());
            StringBuffer queryStr = new StringBuffer(cleanQuery(query.query.toString()));
            queryStr.append(" ");
            for (Pair<String, String> entity : query.entities) {
                queryStr = queryStr.append("taggedContents:\"<" + entity.first + " cl=\\\"" + entity.second +
                        "\\\">\" ");
            }
            System.out.println(queryStr);
            Query q = qp.parse(queryStr.toString());
            Filter f = makeFilter(query);
            if (f != null) {
                indexSearcher.search(q, f, new SrlHitCollector(collector, signal));
            } else {
                indexSearcher.search(q, new SrlHitCollector(collector, signal));
            }
        } catch (Exception x) {
            System.err.println(query.query.toString());
            x.printStackTrace();
        }
    }

    /** Query the corpus */
    public Hits query(String query) throws IOException {
        if (query.equals("")) {
            return null;
        }
        if (indexSearcher == null) {
            closeIndex();
        }
        try {
            QueryParser qp = new QueryParser("contents", processor.getAnalyzer());
            Query q = qp.parse(cleanQuery(query));
            return indexSearcher.search(q);
        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
    }

    /** Make a literal string not cause problems for the indexer, i.e., Put to lower case and bs all reserved terms */
    public static String cleanQuery(String s) {
        s = s.toLowerCase();
        s = s.replaceAll("([\\+\\-\\!\\(\\)\\[\\]\\^\\\"\\~\\?\\:\\\\\\{\\}\\|\\*]|\\&\\&)", "\\\\$1");
        return s;
    }

    /** Get the names of all the documents in the corpus */
    public Set<String> getDocNames() {
        return docNames;
    }

    private List<String> extractDocNames() throws IOException, IllegalStateException {
        if (indexSearcher == null) {
            closeIndex();
        }
        List<String> rv = new Vector<String>();
        for (int i = 0; i < indexSearcher.maxDoc(); i++) {
            String docName = indexSearcher.doc(i).getField("name").stringValue();
            if(docName.matches("\\w+"))
                rv.add(docName);
        }
        return rv;
    }

    /**
     * Creates a suitable filter for the query.
     */
    protected Filter makeFilter(SrlQuery q) throws IOException, IllegalStateException {
        if(indexSearcher == null) 
            closeIndex();
        HashSet<String> filterDocs = new HashSet<String>();
        for (String wordList : q.wordLists) {
            if (support.wordListToDoc.containsKey(wordList)) {
                filterDocs.addAll(support.wordListToDoc.get(wordList));
            }
        }
        if (filterDocs.isEmpty()) {
            return null;
        }
        BitSet bs = new BitSet(indexSearcher.maxDoc());
        for (int i = 0; i < indexSearcher.maxDoc(); i++) {
            if (filterDocs.contains(indexSearcher.doc(i))) {
                bs.set(i);
            }
        }
        return new BitSetFilter(bs);
    }

    private class BitSetFilter extends Filter {

        BitSet bs;

        BitSetFilter(BitSet bs) {
            this.bs = bs;
        }

        @Override
        public BitSet bits(IndexReader arg0) throws IOException {
            return bs;
        }
    }

    /**
     * Get a particular document
     * @param name Document name
     * @return The document, or null if the document is not in the index
     * @throws java.io.IOException If the corpus was not readable
     */
    protected Document getDoc(String name) throws IOException {
        if (indexSearcher == null) {
            closeIndex();
        }
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for (int i = 0; i < hits.length(); i++) {
                if (hits.doc(i).getField("name").stringValue().equals(name)) {
                    return hits.doc(i);
                }
            }
            return null;
        } catch (org.apache.lucene.queryParser.ParseException x) {
            x.printStackTrace();
            return null;
        }

    }
    
    /**
     * Gets the original text for a document
     * @param name The document name
     * @return The plain text contents
     * @throws java.io.IOException If a disk error occurred
     */
    public String getPlainDocContents(String name) throws IOException {
        return getDoc(name).getField("originalContents").stringValue();
    }
    
    /**
     * Get the document sentence by sentence
     * @param name The document name
     * @return A list of the sentences
     * @throws IOException If a disk error occurred
     */
    public List<String> getDocSentences(String name) throws IOException {
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        Vector<String> rval = new Vector<String>();
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for(int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if(docName.equals(name)) { // Not necessary, but it's nice to set the vector to the correct size
                    rval.setSize(Integer.parseInt(hits.doc(i).getField("sentCount").stringValue()));
                } else {
                    Matcher m = Pattern.compile(".* (\\d+)").matcher(docName);
                    if(!m.matches())
                        throw new RuntimeException("Invalid document name in corpus: " + docName);
                    rval.set(Integer.parseInt(m.group(1)), 
                            hits.doc(i).getField("contents").stringValue());
                }
            }
        } catch (org.apache.lucene.queryParser.ParseException x) {
            x.printStackTrace();
            return null;
        }
        return rval;
    }

    
    public List<String> getDocTaggedContents(String name) throws IOException {
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        Vector<String> rval = new Vector<String>();
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for(int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if(docName.equals(name)) { // Not necessary, but it's nice to set the vector to the correct size
                    rval.setSize(Integer.parseInt(hits.doc(i).getField("sentCount").stringValue()));
                } else {
                    Matcher m = Pattern.compile(".* (\\d+)").matcher(docName);
                    if(!m.matches())
                        throw new RuntimeException("Invalid document name in corpus: " + docName);
                    if(hits.doc(i).getField("taggedContents") != null)
                        rval.set(Integer.parseInt(m.group(1)), 
                            hits.doc(i).getField("contents").stringValue());
                    else
                        rval.set(Integer.parseInt(m.group(1)),"");
                }
            }
        } catch (org.apache.lucene.queryParser.ParseException x) {
            x.printStackTrace();
            return null;
        }
        return rval;
    }
    /**
     * Remove a document from the corpus
     * @param name The name of the document
     * @throws java.io.IOException
     */
    public void removeDoc(String name) throws IOException {
        if (indexWriter == null) {
            reopenIndex();
        }
        indexWriter.deleteDocuments(new Term("name", name.toLowerCase()));
        docNames.remove(name);
    }

    /**
     * Change the contents of document in the corpus. If the document already exists
     * 
     * @param name The name of the document
     * @param contents The new contents
     * @throws java.io.IOException
     */
    public void updateDoc(String name, String contents) throws IOException {
        Document old = getDoc(name);
        if (old != null) {
            if (contents.equals(old.getField("originalContents").stringValue())) {
                return;
            }
        }
        if (indexWriter == null) {
            reopenIndex();
        }
        indexWriter.deleteDocuments(new Term("name", name));
        support.removeDoc(name);
        docNames.remove(name);
        addDoc(name, contents);
    }

    /**
     * Tag the corpus
     * @param ruleSets The set of rules for named entity extraction
     */
    public void tagCorpus(Collection<RuleSet> ruleSets) throws IOException {
        if (isIndexOpen()) {
            closeIndex();
        }
        final HashMap<String, List<HashMap<Entity, SrlMatchRegion>>> allMatches =
                new HashMap<String, List<HashMap<Entity, SrlMatchRegion>>>();
        for (RuleSet ruleSet : ruleSets) {
            for (final Pair<String, Rule> rulePair : ruleSet.rules) {
                query(rulePair.second.getCorpusQuery(), new QueryHit() {

                    public void hit(Document d, StopSignal signal) {
                        String name = d.getField("name").stringValue();
                        if (allMatches.get(name) == null) {
                            allMatches.put(name, new LinkedList<HashMap<Entity, SrlMatchRegion>>());
                        }
                        allMatches.get(name).addAll(rulePair.second.getMatch(new SrlDocument(d, processor, false), false));
                    }
                });
            }
        }
        reopenIndex();
        IndexReader reader = IndexReader.open(indexWriter.getDirectory());
        for (Map.Entry<String, List<HashMap<Entity, SrlMatchRegion>>> entry : allMatches.entrySet()) {
            Term t = new Term("name", entry.getKey().toLowerCase());
            TermDocs td = reader.termDocs(t);
            if (!td.next()) {
                throw new RuntimeException("Lost document: " + entry.getKey());
            }
            Document old = reader.document(td.doc());
            Document newDoc = new Document();
            newDoc.add(new Field("name", old.getField("name").stringValue(), Field.Store.YES, Field.Index.TOKENIZED));
            newDoc.add(new Field("contents", old.getField("contents").stringValue(), Field.Store.YES, Field.Index.TOKENIZED));
            String taggedContents = addEntities(new SrlDocument(old, processor, false), entry.getValue());
            System.out.println(taggedContents);
            newDoc.add(new Field("taggedContents", taggedContents, Field.Store.YES, Field.Index.TOKENIZED));

            indexWriter.updateDocument(t, newDoc);
        }
        closeIndex();
        reader.close();

    }

    /** Reinitialize the corpus support
     * @throws java.io.IOException
     */
    public void resupport() throws IOException {
        CorpusSupport newSupport = new CorpusSupport();
        for (String name : WordList.getAllWordListNames()) {
            for (WordList.Entry wle : WordList.getWordList(name)) {
                newSupport.addWord(name, wle.toString(), this);
            }
        }
        support = newSupport;
    }

    /**
     * Add Named Entity Tags
     */
    private String addEntities(SrlDocument sentence, List<HashMap<Entity, SrlMatchRegion>> matches) {
        List<String> tokens = new LinkedList<String>();
        for (org.apache.lucene.analysis.Token tk : sentence) {
            tokens.add(tk.termText());
        }
        List<List<String>> begins = new LinkedList<List<String>>();
        List<List<String>> ends = new LinkedList<List<String>>();
        for(int i = 0; i < tokens.size(); i++) {
            ends.add(new LinkedList<String>());
            begins.add(new LinkedList<String>());
        }
        for (HashMap<Entity, SrlMatchRegion> match : matches) {
            for (Map.Entry<Entity, SrlMatchRegion> entry : match.entrySet()) {
                begins.get(entry.getValue().beginRegion).add("<" +
                        entry.getKey().entityType + " cl=\"" +
                        entry.getKey().entityValue + "\">");
                ends.get(entry.getValue().endRegion + 1).add("</" +
                        entry.getKey().entityType + ">");
            }
        }
        int offset = 0;
        for(int i = 0; i < begins.size(); i++) {
            for(String s : begins.get(i)) {
                tokens.add(i+offset++,s);
            }
        }
        for(int i = 0; i < begins.size(); i++) {
            int offset2 = offset-1;
            for(String s : ends.get(i))  {
                tokens.add(i+offset2,s);
                offset++;
            }
        }
        
        //int offset = 0;
        // TODO: Check for overlapping matches
        /*for (HashMap<Entity, SrlMatchRegion> match : matches) {
            for (Map.Entry<Entity, SrlMatchRegion> entry : match.entrySet()) {
                tokens.add(entry.getValue().beginRegion + offset++, "<" +
                        entry.getKey().entityType + " cl=\"" +
                        entry.getKey().entityValue + "\">");
                tokens.add(entry.getValue().endRegion + 1, "</" +
                        entry.getKey().entityType + ">");
                for (HashMap<Entity, SrlMatchRegion> match2 : matches) {
                    for (Map.Entry<Entity, SrlMatchRegion> entry2 : match2.entrySet()) {
                        int overlap1, overlap2, nest1, nest2;
                        if (entry == entry2) {
                            continue;
                        }
                        overlap1 = overlap2 = nest1 = nest2 = 0;
                        if (entry2.getValue().beginRegion >= entry.getValue().beginRegion) {
                            entry2.getValue().beginRegion++;
                            nest1++;
                            overlap2++;
                        } else {
                            nest2++;
                            overlap1++;
                        }
                        if (entry2.getValue().endRegion >= entry.getValue().endRegion) {
                            entry2.getValue().endRegion++;
                            nest2++;
                            overlap2++;
                        } else {
                            nest1++;
                            overlap1++;
                        }
                        if (entry2.getValue().beginRegion >= entry.getValue().endRegion) {
                            entry2.getValue().beginRegion++;
                            overlap1++;
                            nest2++;
                        } else {
                            nest1++;
                            overlap1++;
                        }
                        if (entry2.getValue().endRegion >= entry.getValue().beginRegion) {
                            entry2.getValue().endRegion++;
                            nest1++;
                            overlap2++;
                        } else {
                            nest2++;
                            overlap1++;
                        }
                        if (!nestingAllowed && (nest1 == 4 || nest2 == 4)) {
                            throw new RuntimeException("Nesting dected");
                        }
                        if (!overlappingAllowed && (overlap1 == 4 || overlap2 == 4)) {
                            throw new RuntimeException("Overlapping detected");
                        }
                    }
                }
            }
        }*/
        return Strings.join(" ", tokens);
    }

    /** Is the corpus open for indexing */
    public boolean isIndexOpen() {
        return indexWriter != null;
    }

    /** Get the name of the class of the analyzer */
    public Processor getProcessor() {
        return processor;
    }

    /** Add this as a listener to list */
    public void listenToWordListSet(WordList list) {
        list.wordLists.addCollectionChangeListener(new CollectionChangeListener<ListenableSet<WordList.Entry>>() {

            public void collectionChanged(CollectionChangeEvent<ListenableSet<WordList.Entry>> e) {

            }
        });
    }

    /** Add this as a listener to wordList */
    public void listenToWordList(String name, ListenableSet<WordList.Entry> wordList) {
        wordList.addCollectionChangeListener(new WLCCL(name));
    }

    private class WLCCL implements CollectionChangeListener<WordList.Entry> {

        String name;

        WLCCL(String name) {
            this.name = name;
        }

        public void collectionChanged(CollectionChangeEvent<WordList.Entry> e) {
            removeWordListElement(name, e.getOldVal() != null ? e.getOldVal().toString() : null);
            addWordListElement(name, e.getNewVal() != null ? e.getNewVal().toString() : null);
        }
    }

    /** Add this as a listener to list */
    public void listenToRuleSet(RuleSet list) {
        list.rules.addCollectionChangeListener(new CollectionChangeListener<Pair<String, Rule>>() {

            public void collectionChanged(CollectionChangeEvent<Pair<String, Rule>> e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    /** Add this as a listener to rule */
    public void listenToRule(Rule rule) {
        rule.body.addCollectionChangeListener(new CollectionChangeListener<TypeExpr>() {

            public void collectionChanged(CollectionChangeEvent<TypeExpr> e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    protected void removeWordListElement(String name, String oldVal) {
        if (oldVal == null) {
            return;
        }
        Thread t = new Thread(new RWT(name, oldVal));
        t.start();
    }

    private class RWT implements Runnable {

        String name, oldVal;

        RWT(String name, String oldVal) {
            this.name = name;
            this.oldVal = oldVal;
        }

        public void run() {
            support.removeWord(name, oldVal, Corpus.this);
        }
    }

    protected void addWordListElement(String name, String newVal) {
        if (newVal == null) {
            return;
        }
        Thread t = new Thread(new AWT(name, newVal));
        t.start();
    }

    private class AWT implements Runnable {

        String name, newVal;

        AWT(String name, String newVal) {
            this.name = name;
            this.newVal = newVal;
        }

        public void run() {
            support.addWord(name, newVal, Corpus.this);
        }
    }

    // DELETE BEFORE RELEASE
    public static void main(String[] args) {
        try {
            CorpusSupport s = new CorpusSupport();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("support")));
            oos.writeObject(s);
            oos.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}

class CorpusSupport implements Serializable {

    HashMap<String, Set<String>> wordListToDoc;
    HashMap<String, Set<Pair<String, String>>> docToWordList;

    public CorpusSupport() {
        wordListToDoc = new HashMap<String, Set<String>>();
        docToWordList = new HashMap<String, Set<Pair<String, String>>>();
    }

    void addWordListInfo(String docName, Set<Pair<String, String>> wordLists) {
        if (wordLists.isEmpty()) {
            return;
        }
        docToWordList.put(docName, wordLists);
        for (Pair<String, String> word : wordLists) {
            if (!wordListToDoc.containsKey(word.first)) {
                wordListToDoc.put(word.first, new HashSet<String>());
            }
            wordListToDoc.get(word.first).add(docName);
        }
    }
    
    void removeDoc(String docName) {
        Iterator<Map.Entry<String,Set<Pair<String,String>>>> iter = docToWordList.entrySet().iterator();
        while(iter.hasNext()) {
            if(iter.next().getKey().matches(docName + " .+"))
                iter.remove();
        }
        for(Set<String> docs : wordListToDoc.values()) {
            Iterator<String> docIter = docs.iterator();
            while(docIter.hasNext()) {
                if(docIter.next().matches(docName+ " .+"))
                    docIter.remove();
            }
        }
    }

    void removeWordList(String wordListName) {
        Set<String> removed = wordListToDoc.remove(wordListName);
        for (String doc : removed) {
            List<Pair<String, String>> toRemove = new LinkedList<Pair<String, String>>();
            for (Pair<String, String> wordList : docToWordList.get(doc)) {
                if (wordList.first.equals(wordListName)) {
                    toRemove.add(wordList);
                }
            }
            docToWordList.get(doc).removeAll(toRemove);
        }
    }

    void removeWord(String wordListName, String word, Corpus c) {
        for (Set<Pair<String, String>> wordList : docToWordList.values()) {
            wordList.remove(new Pair<String, String>(wordListName, word));
        }
        List<String> toRemove = new LinkedList<String>();
        if (!wordListToDoc.containsKey(wordListName)) {
            return;
        }
        for (String docName : wordListToDoc.get(wordListName)) {
            try {
                if (c.wordListForDoc(c.getDoc(docName).getField("contents").stringValue()).isEmpty()) {
                    toRemove.add(docName);
                }
            } catch (IOException x) {
                x.printStackTrace();
            }
        }
        wordListToDoc.get(wordListName).removeAll(toRemove);
    }

    void addWord(String wordListName, String word, Corpus c) {
        try {
            Hits hits = c.query(word);
            if (hits == null) {
                return;
            }
            HashSet<String> docs = new HashSet<String>();
            for (int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if (!docToWordList.containsKey(docName)) {
                    docToWordList.put(docName, new HashSet<Pair<String, String>>());
                }
                docToWordList.get(docName).add(new Pair(wordListName, word));
                docs.add(docName);
            }
            if (!wordListToDoc.containsKey(wordListName)) {
                wordListToDoc.put(wordListName, new HashSet<String>());
            }
            wordListToDoc.get(wordListName).addAll(docs);
        } catch (IOException x) {
            x.printStackTrace();
        }
    }
}
 