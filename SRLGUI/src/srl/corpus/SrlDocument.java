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

import java.io.IOException;
import java.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;

/**
 * @author John McCrae, National Institute of Informatics
 */
public class SrlDocument extends AbstractList<Token> {

    TokenStream stream;
    List<Token> tokensRead;
    int readPoint;
    String name;

    public SrlDocument(Document doc, Processor processor, boolean tagged) {
        try {
            init(doc.getField(tagged ? "taggedContents" : "contents").stringValue(), processor);
        } catch(NullPointerException x) {
            if(tagged) {
                init("",processor);
            } else {
                x.printStackTrace();
                throw new RuntimeException();
            }
        }
        name = doc.getField("name").stringValue();
    }

    public SrlDocument(String name, String contents, Processor processor) {
        init(contents, processor);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private void init(String contents, Processor processor) {
        stream = processor.getTokenStream(contents);
        tokensRead = new LinkedList<Token>();
        readPoint = -1;
    }
    public static HashMap<String, String> analyzerToTokenizerMap = new HashMap<String, String>();

    @Override
    public Iterator<Token> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Token> listIterator(int index) {
        while (index > readPoint+1) {
            try {
                Token t = stream.next();
                if (t == null && index > readPoint) {
                    throw new IndexOutOfBoundsException();
                } else {
                    tokensRead.add((Token)t);
                    readPoint++;
                }
            } catch (IOException x) {
                throw new RuntimeException(x.getMessage());
            }
        }
        return new SrlDocumentIterator(index-1);
    }

    private class SrlDocumentIterator implements ListIterator<Token> {

        int pos;

        public SrlDocumentIterator(int pos) {
            this.pos = pos;
        }

        public void add(Token e) {
            throw new UnsupportedOperationException("Nope");
        }

        public void set(Token e) {
            throw new UnsupportedOperationException("Nope");
        }

        public void remove() {
            throw new UnsupportedOperationException("Nope");
        }

        public boolean hasNext() {
            if (pos == readPoint) {
                try {
                    Token t = stream.next();
                    if (t == null) {
                        return false;
                    } else {
                        tokensRead.add((Token)t);
                        readPoint++;
                        return true;
                    }
                } catch (IOException x) {
                    x.printStackTrace();
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean hasPrevious() {
            return pos > 0;
        }

        public Token next() {
            if (pos == readPoint) {
                try {
                    Token t = stream.next();
                    if (t == null) {
                        throw new IllegalStateException();
                    }
                    readPoint++;
                    pos++;
                    tokensRead.add((Token)t);
                    return (Token)t;
                } catch (IOException x) {
                    x.printStackTrace();
                    throw new RuntimeException();
                }
            } else if (pos < readPoint) {
                return tokensRead.get(++pos);
            } else {
                throw new IllegalStateException();
            }
        }

        public int nextIndex() {
            return pos + 1;
        }

        public Token previous() {
            if (pos == 0) {
                throw new IllegalStateException();
            }
            return tokensRead.get(--pos);
        }

        public int previousIndex() {
            return pos - 1;
        }
    }

    @Override
    public Token get(int index) {
        throw new UnsupportedOperationException("Nah, don't want to do it");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Dunno.");
    }
}
