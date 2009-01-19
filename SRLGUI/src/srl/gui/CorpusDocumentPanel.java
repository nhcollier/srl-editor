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
package srl.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import mccrae.tools.strings.Strings;
import mccrae.tools.struct.Pair;
import org.apache.lucene.analysis.Token;
import srl.corpus.*;
import srl.project.SrlProject;

/**
 *
 * @author  john
 */
public class CorpusDocumentPanel extends javax.swing.JPanel {

    Corpus corpus;
    boolean modified;
    String currentDoc;
    boolean userChange = true;

    /** Creates new form CorpusDocumentPanel */
    public CorpusDocumentPanel() {
        corpus = SRLGUIApp.getApplication().proj.corpus;
        initComponents();
        modified = false;
        for (String doc : corpus.getDocNames()) {
            if (currentDoc == null) {
                currentDoc = doc;
                try {
                    mainPane.setText(corpus.getPlainDocContents(doc));
                } catch (IOException x) {
                    x.printStackTrace();
                } catch (CorpusConcurrencyException x) {
                    x.printStackTrace();
                }

            }
            ((DefaultListModel) docList.getModel()).addElement(doc);
        }
        if(docList.getModel().getSize() > 0) {
            docList.setSelectedIndex(0);
        }
        if(currentDoc == null)
            mainPane.setEditable(false);
        mainPane.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                if(userChange)
                    modified = true;
            }

            public void removeUpdate(DocumentEvent e) {
                if(userChange)
                    modified = true;
            }

            public void changedUpdate(DocumentEvent e) {

            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        saveButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        docList = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        deleteButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        textRadio = new javax.swing.JRadioButton();
        tokenRadio = new javax.swing.JRadioButton();
        tagRadio = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        mainPane = new javax.swing.JTextPane();
        templatesRadio = new javax.swing.JRadioButton();

        setName("Corpus"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(srl.gui.SRLGUIApp.class).getContext().getResourceMap(CorpusDocumentPanel.class);
        saveButton.setIcon(resourceMap.getIcon("saveButton.icon")); // NOI18N
        saveButton.setText(resourceMap.getString("saveButton.text")); // NOI18N
        saveButton.setToolTipText(resourceMap.getString("saveButton.toolTipText")); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        docList.setModel(new DefaultListModel());
        docList.setName("docList"); // NOI18N
        docList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                docListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(docList);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        deleteButton.setIcon(resourceMap.getIcon("deleteButton.icon")); // NOI18N
        deleteButton.setText(resourceMap.getString("deleteButton.text")); // NOI18N
        deleteButton.setToolTipText(resourceMap.getString("deleteButton.toolTipText")); // NOI18N
        deleteButton.setName("deleteButton"); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        addButton.setIcon(resourceMap.getIcon("addButton.icon")); // NOI18N
        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setToolTipText(resourceMap.getString("addButton.toolTipText")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(textRadio);
        textRadio.setSelected(true);
        textRadio.setText(resourceMap.getString("textRadio.text")); // NOI18N
        textRadio.setToolTipText(resourceMap.getString("textRadio.toolTipText")); // NOI18N
        textRadio.setName("textRadio"); // NOI18N
        textRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textRadioActionPerformed(evt);
            }
        });

        buttonGroup1.add(tokenRadio);
        tokenRadio.setText(resourceMap.getString("tokenRadio.text")); // NOI18N
        tokenRadio.setToolTipText(resourceMap.getString("tokenRadio.toolTipText")); // NOI18N
        tokenRadio.setName("tokenRadio"); // NOI18N
        tokenRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tokenRadioActionPerformed(evt);
            }
        });

        buttonGroup1.add(tagRadio);
        tagRadio.setText(resourceMap.getString("tagRadio.text")); // NOI18N
        tagRadio.setToolTipText(resourceMap.getString("tagRadio.toolTipText")); // NOI18N
        tagRadio.setName("tagRadio"); // NOI18N
        tagRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagRadioActionPerformed(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        mainPane.setName("mainPane"); // NOI18N
        jScrollPane1.setViewportView(mainPane);

        buttonGroup1.add(templatesRadio);
        templatesRadio.setText(resourceMap.getString("templatesRadio.text")); // NOI18N
        templatesRadio.setToolTipText(resourceMap.getString("templatesRadio.toolTipText")); // NOI18N
        templatesRadio.setName("templatesRadio"); // NOI18N
        templatesRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                templatesRadioActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(textRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(tokenRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(tagRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(templatesRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 241, Short.MAX_VALUE)
                        .add(addButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(deleteButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(saveButton))
                    .add(jLabel1)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
                    .add(jLabel2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(saveButton)
                            .add(deleteButton))
                        .add(addButton))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(textRadio)
                        .add(tokenRadio)
                        .add(tagRadio)
                        .add(templatesRadio)))
                .add(13, 13, 13))
        );
    }// </editor-fold>//GEN-END:initComponents
    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        try {
            if(docList.getSelectedIndex() == -1)
                return;
            corpus.updateDoc((String) docList.getSelectedValue(), mainPane.getText());
            modified = false;
        } catch (IOException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
        } catch (CorpusConcurrencyException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
        }
         
}//GEN-LAST:event_saveButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        try {
            if(docList.getSelectedIndex() == -1)
                return;
            corpus.removeDoc((String) docList.getSelectedValue());
            ((DefaultListModel) docList.getModel()).remove(docList.getSelectedIndex());
            mainPane.setText("");
            if(docList.getModel().getSize() == 0)
                mainPane.setEditable(false);
            modified = false;
        } catch (IOException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(this, x.getMessage(), "Could not remove from corpus", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void docListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_docListValueChanged
        if (docList.getSelectedIndex() == -1) {
            return;
        }
        if (docList.getSelectedValue().equals(currentDoc)) {
            return;
        }
        if (modified) {
            if (JOptionPane.showConfirmDialog(this, "Save changes to current document?", "Document Modified", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                try {
                    corpus.updateDoc(currentDoc, mainPane.getText());
                    modified = false;
                } catch (IOException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
                    docList.setSelectedValue(currentDoc, false);
                    return;
                } catch (CorpusConcurrencyException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
                    docList.setSelectedValue(currentDoc, false);
                    return;
                }
            }
        }
        currentDoc = docList.getSelectedValue().toString();
        if (textRadio.isSelected()) {
            textRadioActionPerformed(null);
        } else if (tagRadio.isSelected()) {
            tagRadioActionPerformed(null);
        } else if (tokenRadio.isSelected()) {
            tokenRadioActionPerformed(null);
        } else {
            templatesRadioActionPerformed(null);
        }
    }//GEN-LAST:event_docListValueChanged

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        if (modified) {
            if (JOptionPane.showConfirmDialog(this, "Save changes to current document?", "Document Modified", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                try {
                    corpus.updateDoc((String) docList.getSelectedValue(), mainPane.getText());
                    modified = false;
                } catch (IOException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
                    docList.setSelectedValue(currentDoc, false);
                    return;
                } catch (CorpusConcurrencyException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(this, x.getMessage(), "Could not save to corpus", JOptionPane.ERROR_MESSAGE);
                    docList.setSelectedValue(currentDoc, false);
                    return;
                }
            }
        }
        String name = JOptionPane.showInputDialog(this, "Name: ", "");
        if (corpus.getDocNames().contains(name)) {
            JOptionPane.showMessageDialog(this, "Document called " + name + " already exists", "Could not add", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(!name.matches("[a-zA-Z0-9]+")) {
            JOptionPane.showMessageDialog(this, "Document name " + name + " not valid. Must be only alphanumeric.", "Could not add", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            corpus.addDoc(name, "");
        } catch (IOException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(this, x.getMessage(), "Could not add doc to corpus", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ((DefaultListModel) docList.getModel()).addElement(name);
        docList.setSelectedValue(name, true);
    }//GEN-LAST:event_addButtonActionPerformed

    private boolean textSelected = true;
    
    private void textRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textRadioActionPerformed
        userChange = false;
        String docName = (String) docList.getSelectedValue();
        if(docName != null) {
            try {
                mainPane.setText(corpus.getPlainDocContents(docName));
            } catch (IOException x) {
                x.printStackTrace();
                mainPane.setText("<<<<IO Error>>>>");
                return;
            } catch (CorpusConcurrencyException x) {
                x.printStackTrace();
                mainPane.setText("<<<<Corpus Locked: Document contents currently unavailable>>>>");
                return;
            }
            mainPane.setEditable(true);
        }
        textSelected = true;
        userChange = true;
    }//GEN-LAST:event_textRadioActionPerformed

    private void tokenRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tokenRadioActionPerformed
        userChange = false;
        if(textSelected) {
            saveButtonActionPerformed(evt);
        }
        if(docList.getSelectedIndex() != -1) {
            List<String> docTexts;
            try {
                docTexts = corpus.getDocSentences((String)docList.getSelectedValue());
            } catch (IOException x) {
                x.printStackTrace();
                mainPane.setText("<<<<IO Error>>>>");
                return;
            } catch (CorpusConcurrencyException x) {
                x.printStackTrace();
                mainPane.setText("<<<<Corpus Locked: Document contents currently unavailable>>>>");
                return;
            }   
            StringBuffer s = new StringBuffer();
            for(String docText : docTexts) {
                SrlDocument doc = new SrlDocument("", docText, corpus.getProcessor());
                for (Token t : doc) {
                    s.append(t.termText());
                    s.append("\u00b7");
                }
                if(s.length() > 0)
                    s.deleteCharAt(s.length() - 1);
                s.append("\u00b6\n");
            }
            if(s.length() > 0)
                s.deleteCharAt(s.length() - 1);
            mainPane.setText(s.toString());
        }
        mainPane.setEditable(false);
        textSelected = false;
        userChange = true;
    }//GEN-LAST:event_tokenRadioActionPerformed

    private void tagRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagRadioActionPerformed
        userChange = false;
        if(textSelected) {
            saveButtonActionPerformed(evt);
        }
        String docName = (String) docList.getSelectedValue();
        if(docName != null) {
            try {
                addTextStyled(Strings.join("\n", corpus.getDocTaggedContents(docName)));
            } catch (IOException x) {
                x.printStackTrace();
                mainPane.setText("<<<<IO Error>>>>");
            } catch (BadLocationException x) {
                x.printStackTrace();
            } catch (CorpusConcurrencyException x) {
                x.printStackTrace();
                mainPane.setText("<<<<Corpus Locked: Document contents currently unavailable>>>>");
                return;
            }   
        }
        mainPane.setEditable(false);
        textSelected = false;
        userChange = true;
    }//GEN-LAST:event_tagRadioActionPerformed

    static final Color[] colors = { Color.BLUE, Color.RED, Color.GREEN,  Color.PINK, Color.MAGENTA,
        new Color(0xda70d6), new Color(0x800080), new Color(0x00ffff), new Color(0xfa8072), new Color(0x6495ed),
        new Color(0x808000), new Color(0x008080), new Color(0x00ff7f) };
    
    private void addTextStyled(String taggedContents) throws BadLocationException {
        Vector<String> tagNames = new Vector<String>();
        Stack<String> tagStack = new Stack<String>();
        Stack<String> valStack = new Stack<String>();
        StyledDocument doc = mainPane.getStyledDocument();
        mainPane.setText("");
        int idx = -1;
        int oldIdx = 0;
        while((idx = taggedContents.indexOf("<",idx+1)) >= 0) {
            int idx2 = taggedContents.indexOf(">", idx);
            String tag = taggedContents.substring(idx, idx2+1);
            Matcher m = Pattern.compile("</?(\\w+) ?(cl=\"(\\w+)\")?>").matcher(tag);
            if(!m.matches())
                continue;
            String name = m.group(1);
            String val = m.group(3);
            if(tagStack.empty()) {
                doc.insertString(oldIdx, taggedContents.substring(oldIdx, idx), null);
            } else {
                if(val == null)
                    val = valStack.peek();
                Style style = doc.getStyle(name + " " + val);
                if(style == null) {
                    style = doc.addStyle(name + " " + val, null);
                    StyleConstants.setBold(style, true);
                    SrlProject proj = SRLGUIApp.getApplication().proj;
                    if(!proj.entities.contains(new Pair(name,val))) {
                        JOptionPane.showMessageDialog(this, "Unknown tag type " + name
                                + "/" + val + " adding to project", "New tag", 
                                JOptionPane.INFORMATION_MESSAGE);
                        proj.entities.add(new Pair<String,String>(name,val));
                    }
                    StyleConstants.setForeground(style, colors[proj.entities.indexOf(new Pair(name,val)) % colors.length]);
                }
                doc.insertString(oldIdx, taggedContents.substring(oldIdx, idx), style);
            }
            if(m.group(3) == null) {
                idx2 = taggedContents.indexOf(">", idx);
                if(!tagStack.peek().split(" ")[0].equals(name))
                    continue;
                if(!tagStack.empty())
                    doc.insertString(idx, taggedContents.substring(idx, idx2+1), doc.getStyle(tagStack.pop() + " " + valStack.pop()));
                else {
                    System.err.println("Can't colour document");
                    doc.insertString(idx, taggedContents.substring(idx), null);
                    return;
                }
                oldIdx = idx2+1;
            } else {
                tagStack.push(name);
                if(val != null) {
                    valStack.push(val);
                    if(!tagNames.contains(name + " " + val))
                        tagNames.add(name + " " + val);
                }
                oldIdx = idx;
            }
        }
        doc.insertString(oldIdx, taggedContents.substring(oldIdx), null);
    }
    
    private void templatesRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_templatesRadioActionPerformed
        userChange = false;
        if(textSelected) {
            saveButtonActionPerformed(evt);
        }
        String docName = (String) docList.getSelectedValue();
        if(docName != null) {
            try {
                mainPane.setText(Strings.join("\n", corpus.getDocTemplateExtractions(docName)));
            } catch(IOException x) {
                x.printStackTrace();
                mainPane.setText("<<<<IO Error>>>>");
            } catch (CorpusConcurrencyException x) {
                x.printStackTrace();
                mainPane.setText("<<<<Corpus Locked: Document contents currently unavailable>>>>");
                return;
            }   
        }
        mainPane.setEditable(false);
        textSelected = false;
        userChange = true;
    }//GEN-LAST:event_templatesRadioActionPerformed

    public void addDoc(String name) {
        ((DefaultListModel) docList.getModel()).addElement(name);
        mainPane.setEditable(true);
    }
    private String oldText;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton deleteButton;
    private javax.swing.JList docList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane mainPane;
    private javax.swing.JButton saveButton;
    private javax.swing.JRadioButton tagRadio;
    private javax.swing.JRadioButton templatesRadio;
    private javax.swing.JRadioButton textRadio;
    private javax.swing.JRadioButton tokenRadio;
    // End of variables declaration//GEN-END:variables
}
