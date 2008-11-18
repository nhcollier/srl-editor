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
import mccrae.tools.process.ProgressMonitor;
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
    private File indexFile;

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
        c.indexFile = indexFile;
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
        d.add(new Field("uid", generateUID(), Field.Store.YES, Field.Index.TOKENIZED));
        //support.addWordListInfo(name, wordListForDoc(contents.toLowerCase()));
        docNames.add(name);
        int i = 0;
        for (Collection<org.apache.lucene.analysis.Token> sentence : processor.getSplitter().split(new SrlDocument(name, contents, processor), name)) {
            StringBuffer sent = new StringBuffer();
            Iterator<org.apache.lucene.analysis.Token> tkIter = sentence.iterator();
            while (tkIter.hasNext()) {
                sent.append(tkIter.next().termText());
                if (tkIter.hasNext()) {
                    sent.append(" ");
                }
            }
            Document d2 = new Document();
            d2.add(new Field("contents", sent.toString(), Field.Store.YES, Field.Index.TOKENIZED));
            d2.add(new Field("name", name + " " + i, Field.Store.YES, Field.Index.TOKENIZED));
            d2.add(new Field("uid", generateUID(), Field.Store.YES, Field.Index.TOKENIZED));
            support.addWordListInfo(name + " " + i, wordListForDoc(sent.toString()));
            indexWriter.addDocument(d2);
            i++;
        }
        d.add(new Field("sentCount", i + "", Field.Store.YES, Field.Index.NO));
        indexWriter.addDocument(d);
    }
    private HashSet<String> uids = new HashSet<String>();
    private Random random = new Random();

    private String generateUID() {
        String s;
        do {
            s = random.nextLong() + "";
        } while (uids.contains(s));
        uids.add(s);
        return s;
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


        if (dir instanceof RAMDirectory) {
            dir = new RAMDirectory();
        } else {
            dir = FSDirectory.getDirectory(indexFile);
        }

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

    /** Derive an object from this class to provide a callback on
     * query success */
    public interface QueryHit {

        /** The callback function
         * @param d The document hit
         * @param signal Allows premature halt of the query. (If you don't
         * know what this is ignore it, it's really not that important) */
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
            nonLuceneQuery(query, collector, signal);
            return;
        }
        try {
            QueryParser qp = new QueryParser("contents", processor.getAnalyzer());
            qp.setDefaultOperator(QueryParser.Operator.AND);
            StringBuffer queryStr = new StringBuffer(cleanQuery(query.query.toString()));
            queryStr.append(" ");
            for (Pair<String, String> entity : query.entities) {
                queryStr = queryStr.append("taggedContents:\"<" + entity.first + " cl=\\\"" + entity.second +
                        "\\\">\" ");
            }
            Query q = qp.parse(queryStr.toString());
            if (q.toString().matches("\\s*")) {
                nonLuceneQuery(query, collector, signal);
                return;
            }
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

    private void nonLuceneQuery(SrlQuery query, QueryHit collector, StopSignal signal) throws IOException {
        if (query.wordLists.isEmpty()) {
            if (query.entities.isEmpty()) {
                // Empty queries match everything (!)
                System.out.println("Empty Query! This may significantly affect performance");
                for (int i = 0; i < indexSearcher.maxDoc(); i++) {
                    if (indexSearcher.doc(i).getField("contents") != null) {
                        collector.hit(indexSearcher.doc(i), signal);
                    }
                    if (signal != null && signal.isStopped()) {
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
                if (signal != null && signal.isStopped()) {
                    return;
                }
            }
        }
    }

    public boolean containsDoc(String docName) {
        return docNames.contains(docName);
    }

    /** Query the corpus for a single string
     */
    public Hits query(String query) throws IOException {
        if (query.equals("")) {
            return null;
        }
        if (indexSearcher == null) {
            closeIndex();
        }
        try {
            QueryParser qp = new QueryParser("contents", processor.getAnalyzer());
            qp.setDefaultOperator(QueryParser.Operator.AND);
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
        return new TreeSet<String>(docNames);
    }

    private List<String> extractDocNames() throws IOException, IllegalStateException {
        if (indexSearcher == null) {
            closeIndex();
        }
        List<String> rv = new Vector<String>();
        for (int i = 0; i < indexSearcher.maxDoc(); i++) {
            String docName = indexSearcher.doc(i).getField("name").stringValue();
            String uid = indexSearcher.doc(i).getField("uid").stringValue();
            if (docName.matches("\\w+")) {
                rv.add(docName);
                uids.add(uid);
            }
        }
        return rv;
    }

    /**
     * Creates a suitable filter for the query.
     */
    protected Filter makeFilter(SrlQuery q) throws IOException, IllegalStateException {
        if (indexSearcher == null) {
            closeIndex();
        }
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
            if (filterDocs.contains(indexSearcher.doc(i).getField("name").stringValue())) {
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
        if(indexSearcher == null)
            closeIndex();
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        Vector<String> rval = new Vector<String>();
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for (int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if (docName.equals(name)) { // Not necessary, but it's nice to set the vector to the correct size
                    rval.setSize(Integer.parseInt(hits.doc(i).getField("sentCount").stringValue()));
                } else {
                    Matcher m = Pattern.compile(".* (\\d+)").matcher(docName);
                    if (!m.matches()) {
                        throw new RuntimeException("Invalid document name in corpus: " + docName);
                    }
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

    /** Get the tagged contents (as stored) of the document.
     * @return The tagged contents of the document as a sentence-by-sentence list
     */
    public List<String> getDocTaggedContents(String name) throws IOException {
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        Vector<String> rval = new Vector<String>();
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for (int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if (docName.equals(name)) { // Not necessary, but it's nice to set the vector to the correct size
                    rval.setSize(Integer.parseInt(hits.doc(i).getField("sentCount").stringValue()));
                } else {
                    Matcher m = Pattern.compile(".* (\\d+)").matcher(docName);
                    if (!m.matches()) {
                        throw new RuntimeException("Invalid document name in corpus: " + docName);
                    }
                    if (hits.doc(i).getField("taggedContents") != null) {
                        rval.set(Integer.parseInt(m.group(1)),
                                hits.doc(i).getField("taggedContents").stringValue());
                    } else {
                        rval.set(Integer.parseInt(m.group(1)),
                                hits.doc(i).getField("contents").stringValue());
                    }
                }
            }
        } catch (org.apache.lucene.queryParser.ParseException x) {
            x.printStackTrace();
            return null;
        }
        return rval;
    }

    /**
     * The extracted templates as stored for a document.
     * @return The extracted templates of the document as a sentence-by-sentence list
     */
    public List<String> getDocTemplateExtractions(String name) throws IOException {
        QueryParser qp = new QueryParser("name", processor.getAnalyzer());
        Vector<String> rval = new Vector<String>();
        try {
            Query q = qp.parse("\"" + cleanQuery(name) + "\"");
            Hits hits = indexSearcher.search(q);
            for (int i = 0; i < hits.length(); i++) {
                String docName = hits.doc(i).getField("name").stringValue();
                if (docName.equals(name)) { // Not necessary, but it's nice to set the vector to the correct size
                    rval.setSize(Integer.parseInt(hits.doc(i).getField("sentCount").stringValue()));
                } else {
                    Matcher m = Pattern.compile(".* (\\d+)").matcher(docName);
                    if (!m.matches()) {
                        throw new RuntimeException("Invalid document name in corpus: " + docName);
                    }
                    if (hits.doc(i).getField("extracted") != null) {
                        rval.set(Integer.parseInt(m.group(1)),
                                hits.doc(i).getField("extracted").stringValue());
                    } else {
                        rval.set(Integer.parseInt(m.group(1)), "");
                    }
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

    /** Apply the tagging algorithm.
     * @param sents The document as a list of sentences
     * @param ruleSets The rulesets to apply
     * @param p The linguistic processor
     * @return The document as a list (tags are added as BeginTagToken and 
     * EndTagToken objects
     */
    public static List<SrlDocument> tagSentences(List<SrlDocument> sents, Collection<RuleSet> ruleSets, Processor p) throws IOException {
        final Vector<List<HashMap<Entity, SrlMatchRegion>>> allMatches =
                new Vector<List<HashMap<Entity, SrlMatchRegion>>>(sents.size());
        for (RuleSet ruleSet : ruleSets) {
            for (Pair<String, Rule> rulePair : ruleSet.rules) {
                int i = 0;
                for (Collection<org.apache.lucene.analysis.Token> sent : sents) {
                    allMatches.add(new LinkedList<HashMap<Entity, SrlMatchRegion>>());
                    allMatches.get(i++).addAll(rulePair.second.getMatch((SrlDocument) sent, false));
                }
            }
        }
        List<SrlDocument> rval = new Vector<SrlDocument>(sents.size());
        int i = 0;
        for (List<HashMap<Entity, SrlMatchRegion>> matches : allMatches) {
            rval.add(new SrlDocument("name", addEntities((SrlDocument) sents.get(i), sortMatches(matches)), p));
        }
        return rval;
    }

    /** Used to represent an overlap in tagging */
    public class Overlap {

        public Entity e1,  e2;
        public SrlMatchRegion r1,  r2;

        public Overlap(Pair<Entity, SrlMatchRegion> m1, Pair<Entity, SrlMatchRegion> m2) {
            e1 = m1.first;
            e2 = m2.first;
            r1 = m1.second;
            r2 = m2.second;
        }
    }

    /**
     * Tag the corpus
     * @param overlaps A collection, which this function will add any overlaps it detects to (an overlap is a pair of matches
     * where both matches hit the same token and both matches have one token not matched by the other e.g., [0,4] &amp; [1,5])
     * @param ruleSets The set of rules for named entity extraction
     */
    public void tagCorpus(Collection<RuleSet> ruleSets, Collection<Overlap> overlaps) throws IOException {
        tagCorpus(ruleSets, overlaps, null);
    }

    /**
     * Tag the corpus
     * @param overlaps A collection, which this function will add any overlaps it detects to (an overlap is a pair of matches
     * where both matches hit the same token and both matches have one token not matched by the other e.g., [0,4] &amp; [1,5])
     * @param ruleSets The set of rules for named entity extraction
     * @param monitor Monitors the progress surprisingly
     */
    public void tagCorpus(Collection<RuleSet> ruleSets, Collection<Overlap> overlaps, ProgressMonitor monitor) throws IOException {
        if (isIndexOpen()) {
            closeIndex();
        }
        final HashMap<String, List<HashMap<Entity, SrlMatchRegion>>> allMatches =
                new HashMap<String, List<HashMap<Entity, SrlMatchRegion>>>();
        int i = 0;
        for (RuleSet ruleSet : ruleSets) {
            int j = 0;
            for (final Pair<String, Rule> rulePair : ruleSet.rules) {
                if (monitor != null) {
                    monitor.setMessageVal("Matching rule " + rulePair.first);
                    monitor.setProgressVal((float) (i * ruleSet.rules.size() + j++) / (float) ruleSets.size() / (float) ruleSet.rules.size());
                }
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
            i++;
        }
        reopenIndex();
        IndexReader reader = IndexReader.open(indexWriter.getDirectory());
        i = 0;
        for (Map.Entry<String, List<HashMap<Entity, SrlMatchRegion>>> entry : allMatches.entrySet()) {
            Vector<Pair<Entity, SrlMatchRegion>> matches = findOverlapsAndKill(entry.getValue(), overlaps);
            addTagsToDocument(entry.getKey(), matches, reader, monitor);
            if (monitor != null) {
                monitor.setMessageVal("Updating document " + entry.getKey());
                monitor.setProgressVal((float) i++ / allMatches.size());
            }
        }
        closeIndex();
        reader.close();
        if (monitor != null) {
            monitor.setMessageVal("Corpus tagging complete");
            monitor.setProgressVal(1.0f);
        }
    }
    
    /**
     * (expert) Add a set of tags from an external source. 
     * @param docName The document to add the tags to
     * @param matches The matches (formatted as if it was the result
     * @throws java.io.IOException
     * @throws org.apache.lucene.index.CorruptIndexException
     */
    public void addTagsToDocument(String docName, List<Vector<Pair<Entity,SrlMatchRegion>>> matches) throws IOException, CorruptIndexException {
        reopenIndex();
        IndexReader reader = IndexReader.open(indexWriter.getDirectory());
        int i = 0;
        for(Vector<Pair<Entity,SrlMatchRegion>> match : matches) {
            addTagsToDocument(docName + " " + i, match, reader, null);
            i++;
        }
        closeIndex();
        reader.close();
    }

    private void addTagsToDocument(String docName, Vector<Pair<Entity, SrlMatchRegion>> matches, IndexReader reader, ProgressMonitor monitor)
            throws IOException, CorruptIndexException {
        Term t = new Term("name", docName.toLowerCase().split(" ")[0]);
        int docNo = Integer.parseInt(docName.split(" ")[1]);
        TermDocs td = reader.termDocs(t);
        Document old;
        while (true) {
            if (!td.next()) {
                throw new RuntimeException("Lost document: " + docName);
            }
            old = reader.document(td.doc());
            String dn = old.getField("name").stringValue();
            if (dn.matches(".* .*") && Integer.parseInt(dn.split(" ")[1]) == docNo) {
                break;
            }
        }

        Document newDoc = new Document();
        newDoc.add(new Field("name", old.getField("name").stringValue(), Field.Store.YES, Field.Index.TOKENIZED));
        newDoc.add(new Field("contents", old.getField("contents").stringValue(), Field.Store.YES, Field.Index.TOKENIZED));
        newDoc.add(new Field("uid", old.getField("uid").stringValue(), Field.Store.YES, Field.Index.TOKENIZED));
        String taggedContents = addEntities(new SrlDocument(old, processor, false), matches);
        newDoc.add(new Field("taggedContents", taggedContents, Field.Store.YES, Field.Index.TOKENIZED));
        Term uidT = new Term("uid", old.getField("uid").stringValue());
        indexWriter.updateDocument(uidT, newDoc);
    }

    private Vector<Pair<Entity, SrlMatchRegion>> findOverlapsAndKill(List<HashMap<Entity, SrlMatchRegion>> allMatches,
            Collection<Overlap> overlaps) {
        Vector<Pair<Entity, SrlMatchRegion>> matches = sortMatches(allMatches);
        ListIterator<Pair<Entity, SrlMatchRegion>> mIter = matches.listIterator(matches.size());
        LOOP:
        while (mIter.hasPrevious()) {
            Pair<Entity, SrlMatchRegion> m1 = mIter.previous();
            ListIterator<Pair<Entity, SrlMatchRegion>> mIter2 = matches.listIterator();
            while (mIter2.hasNext()) {
                Pair<Entity, SrlMatchRegion> m2 = mIter2.next();
                if (m2.second.beginRegion < m1.second.beginRegion &&
                        m2.second.endRegion < m1.second.endRegion &&
                        m2.second.beginRegion < m1.second.endRegion &&
                        m2.second.endRegion > m1.second.beginRegion) {
                    mIter2.remove();
                    mIter = matches.listIterator(matches.size());
                    if (overlaps != null) {
                        overlaps.add(new Overlap(m1, m2));
                    }
                    continue LOOP;
                } else if (m2.second.beginRegion == m1.second.beginRegion &&
                        m1.second.endRegion == m2.second.beginRegion &&
                        m1.first.entityType.equals(m2.first.entityType) &&
                        m1.first.entityValue.equals(m2.first.entityValue)) {
                    mIter2.remove();
                    mIter = matches.listIterator(matches.size());
                    continue LOOP;
                }

            }
        }
        return matches;
    }

    /** Reinitialize the corpus support. This is actually research for word 
     * list entry matches, sometimes they get out of sync, I don't know why,
     * hopefully they are fixed now and I just forgot to remove this comment.
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

    /** Sort a selection of matches in order of appearance */
    public static Vector<Pair<Entity, SrlMatchRegion>> sortMatches(List<HashMap<Entity, SrlMatchRegion>> matches) {
        Vector<Pair<Entity, SrlMatchRegion>> rv = new Vector<Pair<Entity, SrlMatchRegion>>(matches.size());
        for (HashMap<Entity, SrlMatchRegion> match : matches) {
            for (Map.Entry<Entity, SrlMatchRegion> entry : match.entrySet()) {
                rv.add(new Pair<Entity, SrlMatchRegion>(entry.getKey(), entry.getValue()));
            }
        }
        Collections.sort(rv, new Comparator() {

            public int compare(Object o1, Object o2) {
                Pair<Entity, SrlMatchRegion> m1 = (Pair<Entity, SrlMatchRegion>) o1, m2 = (Pair<Entity, SrlMatchRegion>) o2;
                if (m1.second.endRegion < m2.second.endRegion) {
                    return -1;
                } else if (m1.second.endRegion > m2.second.endRegion) {
                    return 1;
                } else if (m1.second.beginRegion < m2.second.beginRegion) {
                    return -1;
                } else if (m2.second.beginRegion > m2.second.endRegion) {
                    return 1;
                } else {
                    return m1.first.entityType.compareTo(m2.first.entityType);
                }
            }
        });
        Iterator<Pair<Entity, SrlMatchRegion>> matchIter = rv.iterator();
        if (!matchIter.hasNext()) {
            return rv;
        }
        Pair<Entity, SrlMatchRegion> last = matchIter.next();
        while (matchIter.hasNext()) {
            Pair<Entity, SrlMatchRegion> next = matchIter.next();
            if (next.first == last.first && next.second.beginRegion == last.second.beginRegion && next.second.endRegion == last.second.endRegion) {
                matchIter.remove();
            } else {
                last = next;
            }
        }
        return rv;
    }

    /**
     * Add Named Entity Tags
     */
    private static String addEntities(SrlDocument sentence, Vector<Pair<Entity, SrlMatchRegion>> matches) {
        List<String> tokens = new LinkedList<String>();
        for (org.apache.lucene.analysis.Token tk : sentence) {
            tokens.add(tk.termText());
        }
        List<List<String>> begins = new LinkedList<List<String>>();
        List<List<String>> ends = new LinkedList<List<String>>();
        for (int i = 0; i <= tokens.size(); i++) {
            ends.add(new LinkedList<String>());
            begins.add(new LinkedList<String>());
        }
        ends.add(new LinkedList<String>());
        for (Pair<Entity, SrlMatchRegion> entry : matches) {
            begins.get(entry.second.beginRegion).add(0, "<" +
                    entry.first.entityType + " cl=\"" +
                    entry.first.entityValue + "\">");
            try {
                ends.get(entry.second.endRegion).add("</" +
                        entry.first.entityType + ">");
            } catch (IndexOutOfBoundsException x) {
                System.out.println(sentence.getName());
                x.printStackTrace();
            }
        }
        int offset = 0;
        for (int i = 0; i < ends.size(); i++) {
            for (String s : ends.get(i)) {
                tokens.add(i + offset++, s);
            }
            if (i < begins.size()) {
                for (String s : begins.get(i)) {
                    tokens.add(i + offset++, s);
                }
            }
        }
        return Strings.join(" ", tokens);
    }

    /** Extract all the templates from this corpus */
    public void extractTemplates(Collection<RuleSet> ruleSets) throws IOException {
        extractTemplates(ruleSets, null);
    }

    /** Extract all the templates from this corpus */
    public void extractTemplates(Collection<RuleSet> ruleSets, ProgressMonitor monitor) throws IOException {
        if (isIndexOpen()) {
            closeIndex();
        }
        final HashMap<String, List<String>> allMatches =
                new HashMap<String, List<String>>();
        int i = 0;
        for (RuleSet ruleSet : ruleSets) {
            int j = 0;
            for (final Pair<String, Rule> rulePair : ruleSet.rules) {
                if (monitor != null) {
                    monitor.setMessageVal("Matching rule " + rulePair.first);
                    monitor.setProgressVal((float) (i * ruleSet.rules.size() + j) / (float) ruleSets.size() / (float) ruleSet.rules.size());
                }
                query(rulePair.second.getCorpusQuery(), new QueryHit() {

                    public void hit(Document d, StopSignal signal) {
                        String name = d.getField("uid").stringValue();
                        if (allMatches.get(name) == null) {
                            allMatches.put(name, new LinkedList<String>());
                        }
                        allMatches.get(name).addAll(rulePair.second.getHeads(new SrlDocument(d, processor, true)));
                    }
                });
            }
            i++;
        }
        reopenIndex();
        IndexReader reader = IndexReader.open(indexWriter.getDirectory());
        i = 0;
        for (Map.Entry<String, List<String>> entry : allMatches.entrySet()) {

            TermDocs td = reader.termDocs(new Term("uid", entry.getKey()));
            if (!td.next()) {
                throw new RuntimeException("Lost Document!");
            }
            Document d = reader.document(td.doc());
            if (monitor != null) {
                monitor.setMessageVal("Updating document " + d.getField("name").stringValue());
                monitor.setProgressVal((float) i++ / allMatches.size());
            }
            d.removeFields("extracted");
            d.add(new Field("extracted", Strings.join("\n", entry.getValue()), Field.Store.YES, Field.Index.NO));
            indexWriter.updateDocument(new Term("uid", entry.getKey()), d);
        }
        reader.close();
        closeIndex();
        if (monitor != null) {
            monitor.setMessageVal("Template Extraction complete");
            monitor.setProgressVal(1.0f);
        }
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
    /*
    public static void main(String[] args) {
    try {
    CorpusSupport s = new CorpusSupport();
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("support")));
    oos.writeObject(s);
    oos.close();
    } catch (Exception x) {
    x.printStackTrace();
    }
    }*/
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
        Iterator<Map.Entry<String, Set<Pair<String, String>>>> iter = docToWordList.entrySet().iterator();
        while (iter.hasNext()) {
            if (iter.next().getKey().matches(docName + " .+")) {
                iter.remove();
            }
        }
        for (Set<String> docs : wordListToDoc.values()) {
            Iterator<String> docIter = docs.iterator();
            while (docIter.hasNext()) {
                if (docIter.next().matches(docName + " .+")) {
                    docIter.remove();
                }
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
 