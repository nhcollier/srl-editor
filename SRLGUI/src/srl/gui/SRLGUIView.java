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

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.ChangeEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipException;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.tree.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import mccrae.tools.jar.JarClassLoader;
import mccrae.tools.struct.ListenableSet;
import srl.corpus.Corpus;
import srl.corpus.CorpusConcurrencyException;
import srl.corpus.CorpusExtractor;
import srl.project.SrlProject;
import srl.rule.*;
import srl.wordlist.WordListSet;
import srl.wordlist.WordListEntry;

/**
 * The application's main frame.
 */
public class SRLGUIView extends FrameView {

    private static SRLGUIView singleton;

    public SRLGUIView(SingleFrameApplication app) {
        super(app);
        singleton = this;
        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        ruleSetIcon = resourceMap.getIcon("srl.ruleSetIcon");
        wordListIcon = resourceMap.getIcon("srl.wordListIcon");
        corpusIcon = resourceMap.getIcon("srl.corpusIcon");
        closeTabIcon = resourceMap.getIcon("srl.closeTabIcon");
        searchIcon = resourceMap.getIcon("srl.searchTabIcon");
        copyIcon = resourceMap.getIcon("srl.copyIcon");
        cutIcon = resourceMap.getIcon("srl.cutIcon");
        pasteIcon = resourceMap.getIcon("srl.pasteIcon");

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        JMenuItem cutMenu = new JMenuItem(new DefaultEditorKit.CutAction());
        cutMenu.setText("Cut");
        cutMenu.setMnemonic(KeyEvent.VK_T);
        cutMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cutMenu.setIcon(cutIcon);
        jMenu2.add(cutMenu);

        JMenuItem copyMenu = new JMenuItem(new DefaultEditorKit.CopyAction());
        copyMenu.setText("Copy");
        copyMenu.setMnemonic(KeyEvent.VK_C);
        copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copyMenu.setIcon(copyIcon);
        jMenu2.add(copyMenu);

        JMenuItem pasteMenu = new JMenuItem(new DefaultEditorKit.PasteAction());
        pasteMenu.setText("Paste");
        pasteMenu.setMnemonic(KeyEvent.VK_P);
        pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        pasteMenu.setIcon(pasteIcon);
        jMenu2.add(pasteMenu);

        JButton cutButton = new JButton(new DefaultEditorKit.CutAction());
        cutButton.setText("");
        cutButton.setIcon(cutIcon);
        cutButton.setFocusable(false);
        cutButton.setPreferredSize(new Dimension(28,28));
        jToolBar1.add(cutButton,7);

        JButton copyButton = new JButton(new DefaultEditorKit.CopyAction());
        copyButton.setText("");
        copyButton.setIcon(copyIcon);
        copyButton.setFocusable(false);
        copyButton.setPreferredSize(new Dimension(28,28));
        jToolBar1.add(copyButton,8);

        JButton pasteButton = new JButton(new DefaultEditorKit.PasteAction());
        pasteButton.setText("");
        pasteButton.setIcon(pasteIcon);
        pasteButton.setFocusable(false);
        pasteButton.setPreferredSize(new Dimension(28,28));
        jToolBar1.add(pasteButton,9);

        if(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_TOGGLE").equals("true")) {
             try {
                SRLGUIApp.getApplication().proj = SrlProject.openSrlProject(new File(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_PATH")));
                SrlProject proj = SRLGUIApp.getApplication().proj;
                for (WordListSet wl : proj.wordlists) {
                    proj.corpus.listenToWordListSet(wl);
                    for (String l : wl.getLists()) {
                        proj.corpus.listenToWordList(l, WordListSet.getWordList(l));
                    }
                }
                reloadProject();
            } catch (RuntimeException x) {
                if (x.getMessage().matches("Lock obtain timed out: SimpleFSLock.*")) {
                    if(JOptionPane.showConfirmDialog(this.getFrame(), "Corpus locked! This may occur if SRL Editor failed to shut down properly.\nPlease ensure no other copies of SRL Editor are running.\n Do you wish to clear the lock?",
                            "Corpus Lock", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)
                            == JOptionPane.YES_OPTION) {
                        try {
                            File f = new File(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_PATH") + "/corpus/write.lock");
                            f.delete();
                            SRLGUIApp.getApplication().proj = SrlProject.openSrlProject(new File(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_PATH")));
                            SrlProject proj = SRLGUIApp.getApplication().proj;
                            for (WordListSet wl : proj.wordlists) {
                                proj.corpus.listenToWordListSet(wl);
                                for (String l : wl.getLists()) {
                                    proj.corpus.listenToWordList(l, WordListSet.getWordList(l));
                                }
                            }
                            reloadProject();
                         } catch(Exception x2) {
                           error(x2, "Could not load project");
                       }
                    }
                } else {
                    error(x, "Could not open project");
                }
            } catch (Exception x) {
                error(x, "Could not open project");
            }
        }
        String[] pluginJARs = SRLGUIApp.getApplication().getIndexedPreferences(SRLGUIApp.PLUGIN_LOAD_JAR_KEY);
        String[] pluginClasses = SRLGUIApp.getApplication().getIndexedPreferences(SRLGUIApp.PLUGIN_LOAD_CLASS_KEY);

        for(int i = 0; i < pluginJARs.length; i++) {
            try {
                JarClassLoader jcl = new JarClassLoader(pluginJARs[i]);
                Class c = jcl.loadClass(pluginClasses[i]);
                SRLPlugin instance = (SRLPlugin)c.getConstructor().newInstance();
                SRLGUIApp.getApplication().addPlugin(instance, pluginJARs[i], pluginClasses[i]);
            } catch(IOException x) {
                System.err.println("The JAR file " + pluginJARs[i] + " is missing or corrupted. " +
                        "Removing auto-load for " + pluginClasses[i]);
                x.printStackTrace();
                SRLGUIApp.getApplication().removeIndexedPreference(SRLGUIApp.PLUGIN_LOAD_CLASS_KEY, i);
                SRLGUIApp.getApplication().removeIndexedPreference(SRLGUIApp.PLUGIN_LOAD_JAR_KEY, i);
            } catch(Exception x) {
                System.err.println("Error loading plug-in " + pluginClasses[i]);
                x.printStackTrace();
            }
        }

        rightPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent arg0) {
                Component c = rightPane.getSelectedComponent();
                if(c instanceof RuleSetPanel) {
                    jMenuItem20.setEnabled(true);
                    jMenuItem21.setEnabled(true);
                    jMenuItem24.setEnabled(false);
                    jMenuItem25.setEnabled(false);
                } else if(c instanceof WordListPanel) {
                    jMenuItem20.setEnabled(false);
                    jMenuItem21.setEnabled(false);
                    jMenuItem24.setEnabled(true);
                    jMenuItem25.setEnabled(true);
                } else {
                    jMenuItem20.setEnabled(false);
                    jMenuItem21.setEnabled(false);
                    jMenuItem24.setEnabled(false);
                    jMenuItem25.setEnabled(false);
                }
            }
        });
    }

    public static SRLGUIView getView() {
        return singleton;
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SRLGUIApp.getApplication().getMainFrame();
            aboutBox = new SRLGUIAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SRLGUIApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        mainTree = new javax.swing.JTree();
        rightPane = new javax.swing.JTabbedPane();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JToolBar.Separator();
        jSeparator12 = new javax.swing.JToolBar.Separator();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        jButton9 = new javax.swing.JButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        jButton15 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        newProjectMenuItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem23 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenuItem16 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem22 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        jMenuItem20 = new javax.swing.JMenuItem();
        jMenuItem21 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        jMenuItem24 = new javax.swing.JMenuItem();
        jMenuItem25 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JSeparator();
        jMenuItem8 = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem27 = new javax.swing.JMenuItem();
        jMenuItem28 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem26 = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jSeparator14 = new javax.swing.JSeparator();

        mainPanel.setName("mainPanel"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        mainTree.setModel(new DefaultTreeModel(SRLGUIApp.getApplication().getMainTreeNode()));
        mainTree.setEnabled(false);
        mainTree.setName("mainTree"); // NOI18N
        mainTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                mainTreeMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mainTreeMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(mainTree);

        rightPane.setEnabled(false);
        rightPane.setName("rightPane"); // NOI18N

        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(srl.gui.SRLGUIApp.class).getContext().getActionMap(SRLGUIView.class, this);
        jButton1.setAction(actionMap.get("newProject")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(srl.gui.SRLGUIApp.class).getContext().getResourceMap(SRLGUIView.class);
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(28, 28));
        jToolBar1.add(jButton1);

        jButton2.setAction(actionMap.get("openProject")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton2);

        jButton3.setAction(actionMap.get("saveProject")); // NOI18N
        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton3.setName("jButton3"); // NOI18N
        jButton3.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton3);

        jSeparator3.setName("jSeparator3"); // NOI18N
        jToolBar1.add(jSeparator3);

        jButton10.setAction(actionMap.get("undo")); // NOI18N
        jButton10.setText(resourceMap.getString("jButton10.text")); // NOI18N
        jButton10.setFocusable(false);
        jButton10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton10.setName("jButton10"); // NOI18N
        jButton10.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton10.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton10);

        jButton11.setAction(actionMap.get("redo")); // NOI18N
        jButton11.setText(resourceMap.getString("jButton11.text")); // NOI18N
        jButton11.setFocusable(false);
        jButton11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton11.setName("jButton11"); // NOI18N
        jButton11.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton11.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton11);

        jSeparator11.setName("jSeparator11"); // NOI18N
        jToolBar1.add(jSeparator11);

        jSeparator12.setName("jSeparator12"); // NOI18N
        jToolBar1.add(jSeparator12);

        jButton4.setAction(actionMap.get("addRuleSet")); // NOI18N
        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton4.setName("jButton4"); // NOI18N
        jButton4.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton4);

        jButton5.setAction(actionMap.get("addWordList")); // NOI18N
        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton5.setName("jButton5"); // NOI18N
        jButton5.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton5);

        jButton6.setAction(actionMap.get("addCorpusDoc")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton6.setName("jButton6"); // NOI18N
        jButton6.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton6);

        jSeparator4.setName("jSeparator4"); // NOI18N
        jToolBar1.add(jSeparator4);

        jButton7.setAction(actionMap.get("tagCorpus")); // NOI18N
        jButton7.setText(resourceMap.getString("jButton7.text")); // NOI18N
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setMinimumSize(new java.awt.Dimension(20, 20));
        jButton7.setName("jButton7"); // NOI18N
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton7);

        jButton8.setAction(actionMap.get("extractTemplates")); // NOI18N
        jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setName("jButton8"); // NOI18N
        jButton8.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton8);

        jSeparator6.setName("jSeparator6"); // NOI18N
        jToolBar1.add(jSeparator6);

        jButton9.setAction(actionMap.get("searchCorpus")); // NOI18N
        jButton9.setText(resourceMap.getString("jButton9.text")); // NOI18N
        jButton9.setFocusable(false);
        jButton9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton9.setName("jButton9"); // NOI18N
        jButton9.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton9.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton9);

        jSeparator13.setName("jSeparator13"); // NOI18N
        jToolBar1.add(jSeparator13);

        jButton15.setAction(actionMap.get("openWiki")); // NOI18N
        jButton15.setText(resourceMap.getString("jButton15.text")); // NOI18N
        jButton15.setFocusable(false);
        jButton15.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton15.setName("jButton15"); // NOI18N
        jButton15.setPreferredSize(new java.awt.Dimension(28, 28));
        jButton15.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton15);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 213, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(rightPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
                .addContainerGap())
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, mainPanelLayout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, rightPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE))
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setMnemonic('f');
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newProjectMenuItem.setAction(actionMap.get("newProject")); // NOI18N
        newProjectMenuItem.setText(resourceMap.getString("newProjectMenuItem.text")); // NOI18N
        newProjectMenuItem.setName("newProjectMenuItem"); // NOI18N
        fileMenu.add(newProjectMenuItem);

        jMenuItem2.setAction(actionMap.get("openProject")); // NOI18N
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        fileMenu.add(jMenuItem2);

        jMenuItem1.setAction(actionMap.get("saveProject")); // NOI18N
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        fileMenu.add(jMenuItem1);

        jMenuItem23.setAction(actionMap.get("saveProjectAs")); // NOI18N
        jMenuItem23.setName("jMenuItem23"); // NOI18N
        fileMenu.add(jMenuItem23);

        jSeparator2.setName("jSeparator2"); // NOI18N
        fileMenu.add(jSeparator2);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        jMenu2.setMnemonic('e');
        jMenu2.setText(resourceMap.getString("jMenu2.text")); // NOI18N
        jMenu2.setName("jMenu2"); // NOI18N

        jMenuItem15.setAction(actionMap.get("undo")); // NOI18N
        jMenuItem15.setText(resourceMap.getString("jMenuItem15.text")); // NOI18N
        jMenuItem15.setName("jMenuItem15"); // NOI18N
        jMenu2.add(jMenuItem15);

        jMenuItem16.setAction(actionMap.get("redo")); // NOI18N
        jMenuItem16.setText(resourceMap.getString("jMenuItem16.text")); // NOI18N
        jMenuItem16.setName("jMenuItem16"); // NOI18N
        jMenu2.add(jMenuItem16);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jMenu2.add(jSeparator1);

        menuBar.add(jMenu2);

        jMenu3.setMnemonic('r');
        jMenu3.setText(resourceMap.getString("jMenu3.text")); // NOI18N
        jMenu3.setEnabled(false);
        jMenu3.setName("jMenu3"); // NOI18N

        jMenuItem3.setAction(actionMap.get("addRuleSet")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenu3.add(jMenuItem3);

        jMenuItem12.setAction(actionMap.get("importRuleSet")); // NOI18N
        jMenuItem12.setName("jMenuItem12"); // NOI18N
        jMenu3.add(jMenuItem12);

        jMenuItem22.setAction(actionMap.get("deleteRuleSet")); // NOI18N
        jMenuItem22.setName("jMenuItem22"); // NOI18N
        jMenu3.add(jMenuItem22);

        jSeparator5.setName("jSeparator5"); // NOI18N
        jMenu3.add(jSeparator5);

        jMenuItem20.setAction(actionMap.get("addRule")); // NOI18N
        jMenuItem20.setName("jMenuItem20"); // NOI18N
        jMenu3.add(jMenuItem20);

        jMenuItem21.setAction(actionMap.get("removeRule")); // NOI18N
        jMenuItem21.setName("jMenuItem21"); // NOI18N
        jMenu3.add(jMenuItem21);

        menuBar.add(jMenu3);

        jMenu4.setMnemonic('w');
        jMenu4.setText(resourceMap.getString("jMenu4.text")); // NOI18N
        jMenu4.setEnabled(false);
        jMenu4.setName("jMenu4"); // NOI18N

        jMenuItem4.setAction(actionMap.get("addWordList")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        jMenu4.add(jMenuItem4);

        jMenuItem13.setAction(actionMap.get("importWordList")); // NOI18N
        jMenuItem13.setName("jMenuItem13"); // NOI18N
        jMenu4.add(jMenuItem13);

        jSeparator8.setName("jSeparator8"); // NOI18N
        jMenu4.add(jSeparator8);

        jMenuItem24.setAction(actionMap.get("addWordListToSet")); // NOI18N
        jMenuItem24.setName("jMenuItem24"); // NOI18N
        jMenu4.add(jMenuItem24);

        jMenuItem25.setAction(actionMap.get("removeWordListFromSet")); // NOI18N
        jMenuItem25.setName("jMenuItem25"); // NOI18N
        jMenu4.add(jMenuItem25);

        menuBar.add(jMenu4);

        jMenu5.setMnemonic('c');
        jMenu5.setText(resourceMap.getString("jMenu5.text")); // NOI18N
        jMenu5.setEnabled(false);
        jMenu5.setName("jMenu5"); // NOI18N

        jMenuItem5.setAction(actionMap.get("addCorpusDoc")); // NOI18N
        jMenuItem5.setText(resourceMap.getString("jMenuItem5.text")); // NOI18N
        jMenuItem5.setName("jMenuItem5"); // NOI18N
        jMenu5.add(jMenuItem5);

        jMenuItem6.setAction(actionMap.get("tagCorpus")); // NOI18N
        jMenuItem6.setName("jMenuItem6"); // NOI18N
        jMenu5.add(jMenuItem6);

        jMenuItem7.setAction(actionMap.get("extractTemplates")); // NOI18N
        jMenuItem7.setName("jMenuItem7"); // NOI18N
        jMenu5.add(jMenuItem7);

        jSeparator9.setName("jSeparator9"); // NOI18N
        jMenu5.add(jSeparator9);

        jMenuItem8.setAction(actionMap.get("searchCorpus")); // NOI18N
        jMenuItem8.setName("jMenuItem8"); // NOI18N
        jMenu5.add(jMenuItem8);

        jSeparator10.setName("jSeparator10"); // NOI18N
        jMenu5.add(jSeparator10);

        jMenuItem14.setAction(actionMap.get("importTagged")); // NOI18N
        jMenuItem14.setName("jMenuItem14"); // NOI18N
        jMenu5.add(jMenuItem14);

        jMenuItem11.setAction(actionMap.get("writeTagged")); // NOI18N
        jMenuItem11.setName("jMenuItem11"); // NOI18N
        jMenu5.add(jMenuItem11);

        jMenuItem10.setAction(actionMap.get("writeTemplates")); // NOI18N
        jMenuItem10.setName("jMenuItem10"); // NOI18N
        jMenu5.add(jMenuItem10);

        menuBar.add(jMenu5);

        jMenu6.setMnemonic('t');
        jMenu6.setText(resourceMap.getString("jMenu6.text")); // NOI18N
        jMenu6.setName("jMenu6"); // NOI18N

        jMenuItem27.setAction(actionMap.get("openPlugInDialog")); // NOI18N
        jMenuItem27.setName("jMenuItem27"); // NOI18N
        jMenu6.add(jMenuItem27);

        jMenuItem28.setAction(actionMap.get("openSettings")); // NOI18N
        jMenuItem28.setName("jMenuItem28"); // NOI18N
        jMenu6.add(jMenuItem28);

        menuBar.add(jMenu6);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        jMenuItem9.setAction(actionMap.get("openWiki")); // NOI18N
        jMenuItem9.setName("jMenuItem9"); // NOI18N
        helpMenu.add(jMenuItem9);

        jMenuItem26.setAction(actionMap.get("openLanguageDescription")); // NOI18N
        jMenuItem26.setName("jMenuItem26"); // NOI18N
        helpMenu.add(jMenuItem26);

        jSeparator7.setName("jSeparator7"); // NOI18N
        helpMenu.add(jSeparator7);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 558, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        jSeparator14.setName("jSeparator1"); // NOI18N

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents
    private void mainTreeMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainTreeMousePressed
        if (!mainTree.isEnabled()) {
            return;
        }
        if (evt.getClickCount() == 2 && !evt.isPopupTrigger()) {
            final TreePath path = mainTree.getPathForLocation(evt.getX(), evt.getY());
            if (path == null || path.getPath() == null) {
                return;
            }
            if (path.getPath().length == 3 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Template Rules")) {
                String patternSetName = (String) ((DefaultMutableTreeNode) path.getPath()[2]).getUserObject();
                openRuleSetPane(patternSetName, Rule.TEMPLATE_RULE);
            } else if (path.getPath().length == 3 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Entity Rules")) {
                String patternSetName = (String) ((DefaultMutableTreeNode) path.getPath()[2]).getUserObject();
                openRuleSetPane(patternSetName, Rule.ENTITY_RULE);
            } else if (path.getPath().length == 3 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Word List Sets")) {
                String wordListName = (String) ((DefaultMutableTreeNode) path.getPath()[2]).getUserObject();
                openWordListPane(wordListName);
            } else if (path.getPath().length == 2 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Corpus")) {
                openCorpusPane();
            }
        } else if (evt.isPopupTrigger()) {
            onMainTreePopupTrigger(evt);
        }
    }//GEN-LAST:event_mainTreeMousePressed

private void mainTreeMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainTreeMouseReleased
        if(mainTree.isEnabled() && evt.isPopupTrigger()) {
            onMainTreePopupTrigger(evt);
        }
}//GEN-LAST:event_mainTreeMouseReleased

    private void onMainTreePopupTrigger(java.awt.event.MouseEvent evt) {
        final TreePath path = mainTree.getPathForLocation(evt.getX(), evt.getY());
        if (path == null || path.getPath() == null) {
            return;
        }
        if (path.getPath().length == 3 && (((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Template Rules") ||
                ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Entity Rules"))) {
            final int ruleType = ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Entity Rules") ? Rule.ENTITY_RULE : Rule.TEMPLATE_RULE;
            JPopupMenu menu = new JPopupMenu();
            JMenuItem addItem = new JMenuItem();
            addItem.setAction(new AbstractAction("Add rule set") {

                public void actionPerformed(ActionEvent e) {
                    addRuleSet(ruleType);
                }
            });
            menu.add(addItem);
            JMenuItem removeItem = new JMenuItem();
            final String name = ((DefaultMutableTreeNode) path.getPath()[2]).getUserObject().toString();
            removeItem.setAction(new AbstractAction("Remove rule set \"" + name + "\"") {

                public void actionPerformed(ActionEvent e) {
                    SrlProject proj = SRLGUIApp.getApplication().proj;
                    List<RuleSet> ruleSetList = (ruleType == Rule.ENTITY_RULE ? proj.entityRulesets : proj.templateRulesets);
                    Iterator<RuleSet> rsIter = ruleSetList.iterator();
                    RuleSet rs = null;
                    while (rsIter.hasNext()) {
                        RuleSet rs2 = rsIter.next();
                        if (rs2.name.equals(name)) {
                            rs = rs2;
                            break;
                        }
                    }
                    removeRuleSet(ruleType, name,
                            (DefaultMutableTreeNode) path.getLastPathComponent());
                    SRLGUIApp.getApplication().addUndoableEdit(new RemoveRuleSetEdit(name, ruleType, rs));
                }
            });
            menu.add(removeItem);
            JMenuItem openItem = new JMenuItem();
            openItem.setAction(new AbstractAction("Open \"" + name + "\" pane") {

                public void actionPerformed(ActionEvent e) {
                    openRuleSetPane(name, ruleType);
                }
            });
            menu.add(openItem);
            menu.show(mainTree, evt.getX(), evt.getY());
        } else if (path.getPath().length == 2 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Corpus")) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem reInit = new JMenuItem();
            reInit.setAction(new AbstractAction("Re-initialize") {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread(new Runnable() {

                        public void run() {
                            try {
                                SRLGUIApp.getApplication().proj.corpus.resupport();
                                JOptionPane.showMessageDialog(getFrame(), "Corpus re-initialized", "Corpus", JOptionPane.INFORMATION_MESSAGE);
                            } catch (IOException x) {
                                error(x, "Cannot re-initialize corpus");
                            } catch(CorpusConcurrencyException x) {
                                error(x, "Corpus locked");
                            }
                        }
                    });
                    t.start();
                }
            });
            menu.add(reInit);
            JMenuItem openItem = new JMenuItem();
            openItem.setAction(new AbstractAction("Open corpus pane") {

                public void actionPerformed(ActionEvent e) {
                    openCorpusPane();
                }
            });
            menu.add(openItem);
            menu.show(mainTree, evt.getX(), evt.getY());
        } else if (path.getPath().length == 3 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Word List Sets")) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem addItem = new JMenuItem();
            addItem.setAction(new AbstractAction("Add word list") {

                public void actionPerformed(ActionEvent e) {
                    addWordList();
                }
            });
            menu.add(addItem);
            JMenuItem removeItem = new JMenuItem();
            final String name = ((DefaultMutableTreeNode) path.getPath()[2]).getUserObject().toString();
            removeItem.setAction(new AbstractAction("Remove word list \"" + name + "\"") {

                public void actionPerformed(ActionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                    String name = (String)node.getUserObject();
                    WordListSet wls = WordListSet.getWordListSetByName(name);
                    removeWordList((DefaultMutableTreeNode) path.getLastPathComponent());
                    SRLGUIApp.getApplication().addUndoableEdit(new RemoveWordListSetEdit(name, node, wls));
                }
            });
            menu.add(removeItem);
            JMenuItem openItem = new JMenuItem();
            openItem.setAction(new AbstractAction("Open \"" + name + "\" pane") {

                public void actionPerformed(ActionEvent e) {
                    openWordListPane(name);
                }
            });
            menu.add(openItem);
            menu.show(mainTree, evt.getX(), evt.getY());
        } else if (path.getPath().length == 2 && ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Word List Sets")) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem addItem = new JMenuItem();
            addItem.setAction(new AbstractAction("Add word list") {

                public void actionPerformed(ActionEvent e) {
                    addWordList();
                }
            });
            menu.add(addItem);
            menu.show(mainTree, evt.getX(), evt.getY());
        } else if (path.getPath().length == 2 && (((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Template Rules") ||
                ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Entity Rules"))) {
            final int ruleType = ((DefaultMutableTreeNode) path.getPath()[1]).getUserObject().equals("Entity Rules") ? Rule.ENTITY_RULE : Rule.TEMPLATE_RULE;
            JPopupMenu menu = new JPopupMenu();
            JMenuItem addItem = new JMenuItem();
            addItem.setAction(new AbstractAction("Add rule set") {

                public void actionPerformed(ActionEvent e) {
                    addRuleSet(ruleType);
                }
            });
            menu.add(addItem);
            menu.show(mainTree, evt.getX(), evt.getY());
        }

    }

    private void openCorpusPane() {
        JPanel p = getPanel(SRLGUIApp.SRL_CORPUS, "");
        if (p != null) {
            rightPane.setSelectedComponent(p);
        } else {
            Component c = new CorpusDocumentPanel();
            rightPane.addTab("Corpus", corpusIcon, c);
            try {
                rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(SRLGUIApp.SRL_CORPUS, "Corpus",corpusIcon));
            } catch(NoSuchMethodError e) {
                // Java 1.5 compatibility
                rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
            }
            rightPane.setSelectedComponent(c);
        }
    }

    private void openWordListPane(String wordListName) {
        JPanel p = getPanel(SRLGUIApp.SRL_WORDLIST, wordListName);
        if (p != null) {
            rightPane.setSelectedComponent(p);
        } else {
            Component c = new WordListPanel(SRLGUIApp.getApplication().wordLists.get(wordListName));
            rightPane.addTab(wordListName, wordListIcon, c);
            try {
                rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(SRLGUIApp.SRL_WORDLIST, wordListName,wordListIcon));
            } catch(NoSuchMethodError e) {
                // Java 1.5 compatibility
                rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
            }
            rightPane.setSelectedComponent(c);
        }
    }

    private class CloseTabIcon implements Icon {
 
	private final Icon icon;
	private JTabbedPane tabbedPane = null;
	private transient Rectangle position = null;
 
	/**
	 * Creates a new instance of CloseTabIcon.
	 */
	public CloseTabIcon() {
		this.icon = closeTabIcon;
	}
 
	/**
	 * when painting, remember last position painted so we can see if the user clicked on the icon.
	 */
	public void paintIcon(Component component, Graphics g, int x, int y) {
 
		// Lazily create a link to the owning JTabbedPane and attach a listener to it, so clicks on the
		// selector tab can be intercepted by this code.
		if (tabbedPane == null) {
			tabbedPane = (JTabbedPane) component;
 
			tabbedPane.addMouseListener(new MouseAdapter() {
 
				@Override
				public void mouseReleased(MouseEvent e) {
					// asking for isConsumed is *very* important, otherwise more than one tab might get closed!
					if (! e.isConsumed() && position.contains(e.getX(), e.getY())) {
						Component p = tabbedPane.getSelectedComponent();
						if (p instanceof Closeable) {
                                                    if (!((Closeable) p).onClose()) {
                                                        e.consume();
                                                        return;
                                                    }
                                                }
                                                rightPane.remove(p);
						e.consume();
					}
				}
			});
		}
 
		position = new Rectangle(x, y, getIconWidth(), getIconHeight());
		icon.paintIcon(component, g, x, y);
	}
 
	/**
	 * just delegate
	 */
	public int getIconWidth() {
		return icon.getIconWidth();
	}
 
	/**
	 * just delegate
	 */
	public int getIconHeight() {
		return icon.getIconHeight();
	}
 
    }
    
    private void openRuleSetPane(String ruleSetName, int ruleType) {
        JPanel p = getPanel(ruleType + 1, ruleSetName);
        if (p != null) {
            rightPane.setSelectedComponent(p);
        } else {
            RuleSet rs;
            if (ruleType == Rule.ENTITY_RULE) {
                rs = SRLGUIApp.getApplication().entityRuleSets.get(ruleSetName);
            } else {
                rs = SRLGUIApp.getApplication().templateRuleSets.get(ruleSetName);
            }
            Component c = new RuleSetPanel(rs);
            rightPane.addTab(ruleSetName, ruleSetIcon, c);
            try {
                rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(ruleType + 1, ruleSetName,ruleSetIcon));
            } catch(NoSuchMethodError e) {
                // Java 1.5 compatibility
                rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
            }
            rightPane.setSelectedComponent(c);
        }
    }
    
    void openShowDocPane(String docName, TreeSet<DocHighlight> highlights, int mode, String msg) {
        ShowDocPanel p = new ShowDocPanel(docName, highlights, mode, msg);
        rightPane.addTab(docName + " (" + msg + ")", p);
        try {
            rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(SRLGUIApp.SRL_SHOW_DOC, p.name,corpusIcon));
        } catch(NoSuchMethodError e) {
            // Java 1.5 compatibility
            rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
        }
        rightPane.setSelectedComponent(p);
    }

    private void error(Exception x, String title) {
        x.printStackTrace();
        JOptionPane.showMessageDialog(getFrame(), x.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    public JPanel getPanel(int type, String name) {
        for (int i = 0; i < rightPane.getTabCount(); i++) {
            Component c = rightPane.getComponentAt(i);
            switch (type) {
                case SRLGUIApp.SRL_ENTITY_RULESET:
                    if (c instanceof RuleSetPanel && ((RuleSetPanel) c).ruleSet.name.equals(name) && ((RuleSetPanel) c).ruleSet.ruleType == Rule.ENTITY_RULE) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_TEMPLATE_RULESET:
                    if (c instanceof RuleSetPanel && ((RuleSetPanel) c).ruleSet.name.equals(name) && ((RuleSetPanel) c).ruleSet.ruleType == Rule.TEMPLATE_RULE) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_WORDLIST:
                    if (c instanceof WordListPanel && ((WordListPanel) c).wl.name.equals(name)) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_CORPUS:
                    if (c instanceof CorpusDocumentPanel) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_PROJECT:
                    if (c instanceof ProjectPanel) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_SEARCH:
                    if (c instanceof SearchPanel && ((SearchPanel)c).name.equals(name)) {
                        return (JPanel) c;
                    }
                    break;
                case SRLGUIApp.SRL_SHOW_DOC:
                    if (c instanceof ShowDocPanel && ((ShowDocPanel)c).name.equals(name)) {
                        return (JPanel) c;
                    }
                    break;
                default:
                    for(SRLPlugin plugin : SRLGUIApp.getApplication().plugins) {
                        if(plugin.panelIs(c, name))
                            return (JPanel)c;
                    }
            }
        }
        return null;
    }

    public void addPanel(String title, JPanel c, int id) {
        rightPane.addTab(title, ruleSetIcon, c);
            try {
                rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(id,title,ruleSetIcon));
            } catch(NoSuchMethodError e) {
                // Java 1.5 compatibility
                rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
            }
            rightPane.setSelectedComponent(c);
    }

    public void closeTab(int type, String name) {
        JPanel p = getPanel(type, name);
        if (p instanceof Closeable) {
            if (!((Closeable) p).onClose()) {
                return;
            }
        }
        rightPane.remove(p);
    }
    JFileChooser jfc;

    @Action
    public void openPatternFile() {
        String[] opts = {"Entity", "Template"};
        int ruleType = JOptionPane.showOptionDialog(this.getFrame(), "Open as entity or template rule set?", "Open rule set", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opts, "Entity");
        if (ruleType == JOptionPane.CLOSED_OPTION) {
            return;
        }
        if (jfc == null) {
            jfc = new JFileChooser();
        }
        if (jfc.showOpenDialog(this.getFrame()) == JFileChooser.APPROVE_OPTION) {
            RuleSet ps;
            try {
                ps = RuleSet.loadFromFile(jfc.getSelectedFile(), ruleType);
            } catch (Exception x) {
                error(x, "Could not open file");
                return;
            }
            if (ruleType == Rule.ENTITY_RULE) {
                SRLGUIApp.getApplication().entityRuleSets.put(ps.name, ps);
            } else {
                SRLGUIApp.getApplication().templateRuleSets.put(ps.name, ps);
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(ps.name);
            DefaultMutableTreeNode ruleSet = (ruleType == Rule.ENTITY_RULE ? SRLGUIApp.getApplication().entityRules : SRLGUIApp.getApplication().templateRules);
            ((DefaultTreeModel) mainTree.getModel()).insertNodeInto(node,
                    ruleSet,
                    ruleSet.getChildCount());
            mainTree.scrollPathToVisible(new TreePath(node.getPath()));

        }
    }

    @Action
    public void newProject() {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        if (proj != null && proj.isModified()) {
            int option = JOptionPane.showConfirmDialog(SRLGUIApp.getApplication().getMainFrame(),
                    "Current project has been modified, do you want save?", "SRL Project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                try {
                    proj.writeProject();
                    SRLGUIApp.getApplication().clearModified();
                } catch (IOException x) {
                    error(x, "Could not save project");
                    return;
                } catch (CorpusConcurrencyException x) {
                    error(x, "Could not save project");
                    return;
                }
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        if(proj != null) {
            try {
                proj.corpus.closeCorpus();
            } catch(IOException x) {
                x.printStackTrace();
                error(x, "Cannot close project");
                return;
            }
        }
        NewProjectDialog dial = new NewProjectDialog(this.getFrame(), true);
        if (jfc == null) {
            jfc = new JFileChooser();
        }
        if (jfc.getSelectedFile() != null) {
            dial.setDefaultFile(jfc.getSelectedFile().getPath());
        }
        dial.setVisible(true);
        if (dial.returnVal) {
            try {
                proj = new SrlProject(dial.getPath(), dial.getProcessor());
            } catch (Exception x) {
                error(x, "Could not create project");
                return;
            }
            SRLGUIApp.getApplication().proj = proj;
            reloadProject();
            SRLGUIApp.getApplication().setModified();
        }
    }

    private void reloadProject() {
        DefaultTreeModel dtm = (DefaultTreeModel) mainTree.getModel();
        DefaultMutableTreeNode entityRules = SRLGUIApp.getApplication().entityRules;
        DefaultMutableTreeNode templateRules = SRLGUIApp.getApplication().templateRules;
        SRLGUIApp a = SRLGUIApp.getApplication();
        while (entityRules.getChildCount() > 0) {
            dtm.removeNodeFromParent((MutableTreeNode) entityRules.getChildAt(0));
        }
        while (templateRules.getChildCount() > 0) {
            dtm.removeNodeFromParent((MutableTreeNode) templateRules.getChildAt(0));
        }
        DefaultMutableTreeNode wordlists = SRLGUIApp.getApplication().wordList;
        a.wordLists.clear();
        while (wordlists.getChildCount() > 0) {
            dtm.removeNodeFromParent((MutableTreeNode) wordlists.getChildAt(0));
        }
        SrlProject proj = SRLGUIApp.getApplication().proj;
        a.templateRuleSets.clear();
        a.entityRuleSets.clear();
        if (proj != null) {
            for (WordListSet wl : proj.wordlists) {
                dtm.insertNodeInto(new DefaultMutableTreeNode(wl.name), wordlists, wordlists.getChildCount());
                a.wordLists.put(wl.name, wl);
            }
            for (RuleSet rs : proj.entityRulesets) {
                dtm.insertNodeInto(new DefaultMutableTreeNode(rs.name), entityRules, entityRules.getChildCount());
                a.entityRuleSets.put(rs.name, rs);
            }
            for (RuleSet rs : proj.templateRulesets) {
                dtm.insertNodeInto(new DefaultMutableTreeNode(rs.name), templateRules, templateRules.getChildCount());
                a.templateRuleSets.put(rs.name, rs);
            }
        }
        rightPane.removeAll();
        mainTree.setEnabled(true);
        jButton4.setEnabled(true);
        jButton5.setEnabled(true);
        jButton6.setEnabled(true);
        jButton7.setEnabled(true);
        jButton8.setEnabled(true);
        jButton9.setEnabled(true);
        jMenu3.setEnabled(true);
        jMenu4.setEnabled(true);
        jMenu5.setEnabled(true);
        rightPane.setEnabled(true);
        rightPane.add(new ProjectPanel(proj));
        SRLGUIApp.getApplication().clearAllEdits();
 //       SRLGUIApp.getApplication().addUndoableEdit(new NullEdit());
    }

    @Action
    public void openProject() {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        if (proj != null && proj.isModified()) {
            int option = JOptionPane.showConfirmDialog(SRLGUIApp.getApplication().getMainFrame(),
                    "Current project has been modified, do you want save?", "SRL Project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                try {
                    proj.writeProject();
                    SRLGUIApp.getApplication().clearModified();
                } catch (IOException x) {
                    error(x, "Could not save project");
                    return;
                } catch (CorpusConcurrencyException x) {
                    error(x, "Could not save project");
                    return;
                }
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        try {
            if(proj != null)
                proj.corpus.closeCorpus(); 
        } catch(IOException x) {
            x.printStackTrace();
            error(x, "Cannot close project");
            return;
        }
        if (jfc == null) {
            jfc = new JFileChooser();
        }
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        try {
        if (jfc.showOpenDialog(this.getFrame()) == JFileChooser.APPROVE_OPTION) {
            try {
                SRLGUIApp.getApplication().proj = SrlProject.openSrlProject(jfc.getSelectedFile());
                proj = SRLGUIApp.getApplication().proj;
                for (WordListSet wl : proj.wordlists) {
                    proj.corpus.listenToWordListSet(wl);
                    for(String l : wl.getLists()) {
                        proj.corpus.listenToWordList(l, WordListSet.getWordList(l));
                    }
                }
                reloadProject();
            } catch (RuntimeException x) {
                if (x.getMessage().matches("Lock obtain timed out: SimpleFSLock.*")) {
                    if(JOptionPane.showConfirmDialog(this.getFrame(), "Corpus locked! This may occur if SRL Editor failed to shut down properly.\nPlease ensure no other copies of SRL Editor are running.\n Do you wish to clear the lock?",
                            "Corpus Lock", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)
                            == JOptionPane.YES_OPTION) {
                        try {
                            File f = new File(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_PATH") + "/corpus/write.lock");
                            f.delete();
                            SRLGUIApp.getApplication().proj = SrlProject.openSrlProject(new File(SRLGUIApp.getApplication().getPreference("ON_START_LOAD_PROJECT_PATH")));
                            for (WordListSet wl : proj.wordlists) {
                                proj.corpus.listenToWordListSet(wl);
                                for (String l : wl.getLists()) {
                                    proj.corpus.listenToWordList(l, WordListSet.getWordList(l));
                                }
                            }
                            reloadProject();
                         } catch(Exception x2) {
                           error(x2, "Could not load project");
                       }
                    }
                } else {
                    error(x, "Could not open project");
                }
            } catch (Exception x) {
                error(x, "Could not open project");
            }
        }
        } finally {
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }
    }

    @Action
    public void saveProject() {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        if (proj != null && proj.isModified()) {
            try {
                proj.writeProject();
                    SRLGUIApp.getApplication().clearModified();
            } catch (IOException x) {
                error(x, "Could not save project");
            } catch (CorpusConcurrencyException x) {
                error(x, "Could not save project");
            }
        }
    }

    @Action
    public void addRuleSet() {
        String[] opts = {"Entity", "Template"};
        int ruleType = JOptionPane.showOptionDialog(this.getFrame(), "Open as entity or template rule set?", "Open rule set", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opts, "Entity");
        if (ruleType == JOptionPane.CLOSED_OPTION) {
            return;
        }
        addRuleSet(ruleType);
    }

    public void addRuleSet(int ruleType) {
        String name = JOptionPane.showInputDialog(this.getFrame(), "Rule Set Name: ");
        if (name == null) {
            return;
        }
        if(name.matches(".*[<>:\"/\\\\\\|\\?\\*].*") ||
                name.matches(".*\\s.*") ||
                name.equals("")) {
            JOptionPane.showMessageDialog(getFrame(), "Rule set name cannot contain whitespace or the following characters: < > : \" \\ | ? *", 
                    "Invalid rule set name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SrlProject proj = SRLGUIApp.getApplication().proj;
        for(RuleSet rs : ruleType == Rule.ENTITY_RULE ? proj.entityRulesets : proj.templateRulesets) {
            if(rs.name.equals(name)) {
                JOptionPane.showMessageDialog(this.getFrame(), name + " already exists", "Cannot add rule set", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        DefaultMutableTreeNode dmtn = addRuleSet(name, ruleType, new RuleSet(ruleType, name));
        SRLGUIApp.getApplication().addUndoableEdit(new AddRuleSetEdit(name, ruleType, dmtn));
    }

    private DefaultMutableTreeNode addRuleSet(String name, int ruleType, RuleSet rs) {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        
        if (ruleType == Rule.ENTITY_RULE) {
            proj.entityRulesets.add(rs);
        } else {
            proj.templateRulesets.add(rs);
        }
        if (ruleType == Rule.ENTITY_RULE) {
            SRLGUIApp.getApplication().entityRuleSets.put(name, rs);
        } else {
            SRLGUIApp.getApplication().templateRuleSets.put(name, rs);
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        DefaultMutableTreeNode ruleSet = (ruleType == Rule.ENTITY_RULE ? SRLGUIApp.getApplication().entityRules : SRLGUIApp.getApplication().templateRules);
        ((DefaultTreeModel) mainTree.getModel()).insertNodeInto(node,
                ruleSet,
                ruleSet.getChildCount());
        mainTree.scrollPathToVisible(new TreePath(node.getPath()));
        return node;
    }

    public void removeRuleSet(int ruleType, String setName, DefaultMutableTreeNode node) {
        JPanel panel = getPanel(ruleType+1, setName);
        if(panel != null) {
            mainPanel.remove(panel);
        }

        SrlProject proj = SRLGUIApp.getApplication().proj;
        List<RuleSet> ruleSetList = (ruleType == Rule.ENTITY_RULE ? proj.entityRulesets : proj.templateRulesets);
        Iterator<RuleSet> rsIter = ruleSetList.iterator();
        while (rsIter.hasNext()) {
            RuleSet rs = rsIter.next();
            if (rs.name.equals(setName)) {
                rsIter.remove();
                break;
            }
        }
        if (ruleType == Rule.ENTITY_RULE) {
            SRLGUIApp.getApplication().entityRuleSets.remove(setName);
        } else {
            SRLGUIApp.getApplication().templateRuleSets.remove(setName);
        }
        DefaultTreeModel dtm = (DefaultTreeModel) mainTree.getModel();
        dtm.removeNodeFromParent(node);
    }

    @Action
    public void addWordList() {
        String name = JOptionPane.showInputDialog(this.getFrame(), "Word List Set Name: ");
        if (name == null) {
            return;
        }
        if(name.matches(".*\\W.*") ||
                name.equals("")) {
            JOptionPane.showMessageDialog(getFrame(), "Word list name cannot contain non-word characters. (Not A-Z or _)", 
                    "Invalid word list name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SrlProject proj = SRLGUIApp.getApplication().proj;
        for(WordListSet wl : proj.wordlists) {
            if(wl.name.equals(name)) {
                 JOptionPane.showMessageDialog(this.getFrame(), name + " already exists", "Cannot add word list set", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        DefaultMutableTreeNode node = addWordList(name, new WordListSet(name, proj.corpus.getProcessor()));
        SRLGUIApp.getApplication().addUndoableEdit(new AddWordListSetEdit(name, node));
    }

    private DefaultMutableTreeNode addWordList(String name, WordListSet wl) {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        proj.corpus.listenToWordListSet(wl);
        proj.wordlists.add(wl);
        wl.restore();
        SRLGUIApp.getApplication().wordLists.put(name, wl);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        ((DefaultTreeModel) mainTree.getModel()).insertNodeInto(node,
                SRLGUIApp.getApplication().wordList,
                SRLGUIApp.getApplication().wordList.getChildCount());
        mainTree.scrollPathToVisible(new TreePath(node.getPath()));
        return node;
    }

    public void removeWordList(DefaultMutableTreeNode node) {
        String name = node.getUserObject().toString();
        JPanel panel = getPanel(SRLGUIApp.SRL_WORDLIST, name);
        if(panel != null) {
            mainPanel.remove(panel);
        }
        SrlProject proj = SRLGUIApp.getApplication().proj;
        WordListSet wl = SRLGUIApp.getApplication().wordLists.get(name);
        wl.die();
        proj.wordlists.remove(wl);
        SRLGUIApp.getApplication().wordLists.remove(name);
        ((DefaultTreeModel) mainTree.getModel()).removeNodeFromParent(node);
    }

    void addPlugin(SRLPlugin plugin) {
        JMenuItem pluginMenu = plugin.getMenu();
        if(pluginMenu == null)
            return;
        int idx;
        for(idx = 0; idx < jMenu6.getComponentCount(); idx++) {
            if(jMenu6.getComponent(idx) instanceof JSeparator) {
                break;
            }
        }
        if(idx == jMenu6.getComponentCount()) {
            idx = 0;
            jMenu6.add(new JSeparator(),0);
        }
        jMenu6.add(pluginMenu,idx);
    }

    private class CustomEncodingFilter extends javax.swing.filechooser.FileFilter {

        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            return "Plain text (Non-default encoding)";
        }
        
    }
    
    private class NullTask extends Task {

        public NullTask() {
            super(SRLGUIApp.getApplication());
        }

        @Override
        protected Object doInBackground() throws Exception {
            return null;
        }
        
    }
    
    @Action
    public Task addCorpusDoc() {
        try {
            if(jfc != null)
                jfc = new JFileChooser(jfc.getSelectedFile());
            else
                jfc = new JFileChooser();
            jfc.setMultiSelectionEnabled(true);
            jfc.addChoosableFileFilter(new CustomEncodingFilter());
            jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                 @Override
                   public boolean accept(File f) {
                         return true;
                  }

            @Override
            public String getDescription() {
                return "Plain text (" + Charset.defaultCharset().name() + ")";
            }
        });
        String encoding = null;
        if (jfc.showOpenDialog(this.getFrame()) == JFileChooser.APPROVE_OPTION) {
            if(jfc.getFileFilter() instanceof CustomEncodingFilter) {
                encoding = JOptionPane.showInputDialog(this.getFrame(), "Enter encoding (e.g., \"UTF-8\"): ", "");
                if(encoding == null) {
                    jfc.setMultiSelectionEnabled(false);
                    jfc.resetChoosableFileFilters();
                    return new NullTask();
                }
                try {
                    Charset.forName(encoding);
                } catch(Exception x) {
                    JOptionPane.showMessageDialog(this.getFrame(), "Invalid encoding", "Cannot load", JOptionPane.WARNING_MESSAGE);
                    jfc.setMultiSelectionEnabled(false);
                    jfc.resetChoosableFileFilters();
                    return new NullTask();
                }
            }
        }
        File[] sf = jfc.getSelectedFiles();
        return new DocumentLoadThread(encoding, sf, false);
        } finally {
            jfc.setMultiSelectionEnabled(false);
        jfc.resetChoosableFileFilters();

        }
    }

    private class AddCorpusDocTask extends org.jdesktop.application.Task<Object, Void> {
        AddCorpusDocTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to AddCorpusDocTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }
    
    private class DocumentLoadThread extends Task {
        String encoding;
        File[] selectedFiles;
        boolean tagged;
        
        public DocumentLoadThread(String encoding, File[] selectedFiles, boolean tagged)
        {
            super(SRLGUIApp.getApplication());
            this.encoding = encoding;
            this.selectedFiles = selectedFiles;
            this.tagged = tagged;
        }

        
        public Object doInBackground() throws Exception {
            try {
                Corpus corpus = SRLGUIApp.getApplication().proj.corpus;
                long lockID = corpus.reopenIndex(true);
                try {
                    int replaceDoc = 0; // 0=? 1=YES -1=NO
                    JPanel p = getPanel(SRLGUIApp.getApplication().SRL_CORPUS, "");
                    int i = 0;
                    for (File file : selectedFiles) {
                        String fName = file.getName().replaceAll("[^A-Za-z0-9]", "");
                        setMessage("Adding " + fName);
                        setProgress((float)i++ / (float)selectedFiles.length);
                        BufferedReader br;
                        if(encoding == null) {
                            br= new BufferedReader(new FileReader(file));
                        } else {
                            br = new BufferedReader(new InputStreamReader(new FileInputStream(file),encoding));
                        }
                        StringBuffer contents = new StringBuffer();
                        String in = br.readLine();
                        while (in != null) {
                            contents.append(in + "\n");
                            in = br.readLine();
                        }
                        br.close();
                        if(corpus.containsDoc(fName)) {
                            if(replaceDoc == 0) {
                                String[] opts = { "Skip", "Replace", "Skip All", "Replace All" };
                                int opt = JOptionPane.showOptionDialog(SRLGUIApp.getApplication().getMainFrame(), "Document called "+fName+" already exists", "Duplicate Document", 
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
                                if(opt == 2) { replaceDoc = -1; }
                                if(opt == 3) { replaceDoc = 1; }
                                if(opt == 0 || opt == 2) {
                                    corpus.updateDoc(fName, contents.toString());
                                }
                            } else if(replaceDoc == 1) {
                                corpus.updateDoc(fName, contents.toString());
                            }
                        } else
                            corpus.addDoc(fName, contents.toString(), tagged);
                        if (p != null) {
                            ((CorpusDocumentPanel) p).addDoc(fName);
                        }
                    }
                } finally {
                    corpus.closeIndex(lockID);
                }
                corpus.optimizeIndex();
            } catch (Exception x) {
                error(x, "Could not add documents to corpus");
            }
            if(selectedFiles.length > 0) {
                setMessage("All documents added");
                setProgress(1.0f);
            }
            return null;
        }
    }

    @Action
    public Task tagCorpus() {
        return new TagCorpusTask();
    }
    
    public void enableSave() {
        jButton3.setEnabled(true);
        jMenuItem1.setEnabled(true);
        jMenuItem23.setEnabled(true);
    }
    
    public void disableSave() {
        jButton3.setEnabled(false);
        jMenuItem1.setEnabled(false);
    }

    private class TagCorpusTask extends Task implements mccrae.tools.process.ProgressMonitor {

        TagCorpusTask() {
            super(SRLGUIApp.getApplication());
        }

        public Object doInBackground() throws Exception {
            try {
                CorpusExtractor ce = new CorpusExtractor(SRLGUIApp.getApplication().proj.corpus);
                LinkedList<CorpusExtractor.Overlap> overlaps = new LinkedList<CorpusExtractor.Overlap>();
                ce.tagCorpus(SRLGUIApp.getApplication().proj.entityRulesets,overlaps, this);
                if(overlaps.isEmpty())
                    JOptionPane.showMessageDialog(SRLGUIApp.getApplication().getMainFrame(), "Corpus tagging complete", "Corpus tagger", JOptionPane.INFORMATION_MESSAGE);
                else {
                    OverlapMessageDialog omd = new OverlapMessageDialog(SRLGUIApp.getApplication().getMainFrame(), true, overlaps);
                    omd.setVisible(true);
                }
            } catch (IOException x) {
                error(x, "Corpus Tagging Failed");
            }
            return null;
        }

        public void setMessageVal(String s) {
            setMessage(s);
        }

        public void setProgressVal(float f) {
            setProgress(f);
        }
        
        
    }

    @Action
    public Task extractTemplates() {
        return new ExtractTemplatesTask();
    }
    
    private class ExtractTemplatesTask extends Task implements mccrae.tools.process.ProgressMonitor {

        public ExtractTemplatesTask() {
            super(SRLGUIApp.getApplication());
        }
        

        public Object doInBackground() throws Exception {
            try {
                CorpusExtractor ce = new CorpusExtractor(SRLGUIApp.getApplication().proj.corpus);
                ce.extractTemplates(SRLGUIApp.getApplication().proj.templateRulesets, this);
                JOptionPane.showMessageDialog(SRLGUIApp.getApplication().getMainFrame(), "Template Extraction Complete", "Template Extraction", JOptionPane.INFORMATION_MESSAGE);
            } catch(IOException x) {
                error(x, "Corpus Tagging Failed");
            }
            return null;
        }

        public void setMessageVal(String s) {
            setMessage(s);
        }

        public void setProgressVal(float f) {
            setProgress(f);
        }
        
    }

    public static int searchCount = 1;
    
    @Action
    public void searchCorpus() {
        String query = JOptionPane.showInputDialog(getFrame(), "Query", "");
        if(query != null && query.length() != 0) {
            String title = "Search " + searchCount++;
            JPanel c =  new SearchPanel(query, title);
            rightPane.addTab(title, c);
             try {
                rightPane.setTabComponentAt(rightPane.getTabCount() - 1, new CloseTabButton(SRLGUIApp.SRL_SEARCH, title,searchIcon));
            } catch(NoSuchMethodError e) {
                // Java 1.5 compatibility
                rightPane.setIconAt(rightPane.getTabCount() - 1, new CloseTabIcon());
            }
            rightPane.setSelectedComponent(c);
        }
    }

    @Action
    public void openWiki() {
        try {
            Desktop.getDesktop().browse(new URI("http://code.google.com/p/srl-editor/w/list"));
        } catch(Exception x) {
            error(x, "Could not open external browser");
        }
    }

    @Action
    public Task writeTagged() {
        return new WriteTaggedTask(getApplication());
    }

    private class WriteTaggedTask extends org.jdesktop.application.Task<Object, Void> {
        WriteTaggedTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to WriteTaggedTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            jfc.setFileSelectionMode(jfc.DIRECTORIES_ONLY);
            try {
                if(jfc.showOpenDialog(SRLGUIApp.getApplication().getMainFrame()) !=
                        jfc.APPROVE_OPTION)
                    return null;
                File directory = jfc.getSelectedFile();
                Corpus corpus = SRLGUIApp.getApplication().proj.corpus;
                Set<String> docNames = corpus.getDocNames();
                float i = 0;
                for(String docName : docNames) {
                    setMessage("Writing tagged: " + docName);
                    setProgress((float)i++ / (float)docNames.size());
                    List<String> cont = corpus.getDocTaggedContents(docName);
                    PrintStream out = new PrintStream(new File(directory, docName + ".tagged"));
                    for(String c : cont) {
                        out.println(c);
                    }
                    out.close();
                }
                JOptionPane.showMessageDialog(SRLGUIApp.getApplication().getMainFrame(), "Tagged documents written", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch(IOException x) {
                error(x, "Could not write document contents");
            } catch(CorpusConcurrencyException x) {
                error(x, "Could not write document contents");
            } finally {
                jfc.setFileSelectionMode(jfc.FILES_ONLY);
            }
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task writeTemplates() {
        return new WriteTemplatesTask(getApplication());
    }

    private class WriteTemplatesTask extends org.jdesktop.application.Task<Object, Void> {
        WriteTemplatesTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to WriteTemplatesTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            jfc.setFileSelectionMode(jfc.DIRECTORIES_ONLY);
            try {
                if(jfc.showOpenDialog(SRLGUIApp.getApplication().getMainFrame()) !=
                        jfc.APPROVE_OPTION)
                    return null;
                File directory = jfc.getSelectedFile();
                Corpus corpus = SRLGUIApp.getApplication().proj.corpus;
                Set<String> docNames = corpus.getDocNames();
                float i = 0;
                for(String docName : docNames) {
                    setMessage("Writing template: " + docName);
                    setProgress(i++ / (float)docNames.size());
                    List<String> cont = corpus.getDocTemplateExtractions(docName);
                    PrintStream out = new PrintStream(new File(directory, docName + ".templates"));
                    for(String c : cont) {
                        out.println(c);
                    }
                    out.close();
                }
                JOptionPane.showMessageDialog(SRLGUIApp.getApplication().getMainFrame(), "Templates documents written", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch(IOException x) {
                error(x, "Could not write document contents");
            } catch(CorpusConcurrencyException x) {
                error(x, "Could not write document contents");
            } finally {
                jfc.setFileSelectionMode(jfc.FILES_ONLY);
            }
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public void importRuleSet() {
        try {
            jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.getName().matches(".*\\.rule\\.srl") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "SRL rule files (*.rule.srl)";
                }
            });
            if(jfc.showOpenDialog(getFrame()) != jfc.APPROVE_OPTION)
                return;
            String[] opts = {"Entity", "Template"};
            int ruleType = JOptionPane.showOptionDialog(this.getFrame(), "Open as entity or template rule set?", "Open rule set", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opts, "Entity");
            File f = jfc.getSelectedFile();
            String name;
            if(f.getName().matches(".*\\.rule\\.srl"))
                name = f.getName().substring(0, f.getName().length()-9);
            else
                name = f.getName();
            SrlProject proj = SRLGUIApp.getApplication().proj;
            RuleSet rs = RuleSet.loadFromFile(f, ruleType);
            if (ruleType == Rule.ENTITY_RULE) {
                proj.entityRulesets.add(rs);
            } else {
                proj.templateRulesets.add(rs);
            }
           if (ruleType == Rule.ENTITY_RULE) {
                SRLGUIApp.getApplication().entityRuleSets.put(name, rs);
            } else {
                SRLGUIApp.getApplication().templateRuleSets.put(name, rs);
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            DefaultMutableTreeNode ruleSet = (ruleType == Rule.ENTITY_RULE ? SRLGUIApp.getApplication().entityRules : SRLGUIApp.getApplication().templateRules);
            ((DefaultTreeModel) mainTree.getModel()).insertNodeInto(node,
                ruleSet,
                ruleSet.getChildCount());
            mainTree.scrollPathToVisible(new TreePath(node.getPath()));
            SRLGUIApp.getApplication().addUndoableEdit(new ImportRuleSetEdit(name, ruleType, node, rs));
        } catch(Exception x) {
            error(x, "Could not import rule set");
        } finally {
            jfc.resetChoosableFileFilters();
        }
    }

    @Action
    public void importWordList() {
        try {
            jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.getName().matches(".*\\.wordlist\\.srl") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "SRL word list files (*.wordlist.srl)";
                }
            });
            if(jfc.showOpenDialog(getFrame()) != jfc.APPROVE_OPTION)
                return;
            File f = jfc.getSelectedFile();
            String name;
            if(f.getName().matches(".*\\.wordlist\\.srl"))
                name = f.getName().substring(0, f.getName().length()-13);
            else
                name = f.getName();
            if(name.matches(".*\\W.*") || name.length()==0) {
                JOptionPane.showMessageDialog(getFrame(), name + " is not a valid name... name must be only word characters", "Cannot add word list set", JOptionPane.WARNING_MESSAGE);
                return;
            }
            SrlProject proj = SRLGUIApp.getApplication().proj;
            for(WordListSet wl : proj.wordlists) {
               if(wl.name.equals(name)) {
                     JOptionPane.showMessageDialog(this.getFrame(), name + " already exists", "Cannot add word list set", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            WordListSet wl = WordListSet.loadFromFile(f, proj.processor);
            proj.corpus.listenToWordListSet(wl);
            proj.wordlists.add(wl);
            SRLGUIApp.getApplication().wordLists.put(name, wl);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            ((DefaultTreeModel) mainTree.getModel()).insertNodeInto(node,
                    SRLGUIApp.getApplication().wordList,
                    SRLGUIApp.getApplication().wordList.getChildCount());
            mainTree.scrollPathToVisible(new TreePath(node.getPath()));
            SRLGUIApp.getApplication().addUndoableEdit(new ImportWordListSetEdit(name, node, wl));
        } catch(Exception x) {
            error(x, "Could not import word list");
        } finally {
            jfc.resetChoosableFileFilters();
        }
    }

    @Action
    public Task importTagged() {
         try {
            // JOptionPane.showMessageDialog(getFrame(), "This needs fixing... please email jmccrae@nii.ac.jp if this has somehow made it to a release version");
            if (jfc == null) {
               jfc = new JFileChooser();
            } else {
                jfc = new JFileChooser(jfc.getSelectedFile());
            }
            jfc.setMultiSelectionEnabled(true);
            jfc.addChoosableFileFilter(new CustomEncodingFilter());
            jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                 @Override
                   public boolean accept(File f) {
                         return true;
                  }

            @Override
            public String getDescription() {
                return "Plain text (" + Charset.defaultCharset().name() + ")";
            }
        });
        String encoding = null;
        if (jfc.showOpenDialog(this.getFrame()) == JFileChooser.APPROVE_OPTION) {
            if(jfc.getFileFilter() instanceof CustomEncodingFilter) {
                encoding = JOptionPane.showInputDialog(this.getFrame(), "Enter encoding (e.g., \"UTF-8\"): ", "");
                if(encoding == null) {
                    jfc.setMultiSelectionEnabled(false);
                    jfc.resetChoosableFileFilters();
                    return new NullTask();
                }
                try {
                    Charset.forName(encoding);
                } catch(Exception x) {
                    JOptionPane.showMessageDialog(this.getFrame(), "Invalid encoding", "Cannot load", JOptionPane.WARNING_MESSAGE);
                    jfc.setMultiSelectionEnabled(false);
                    jfc.resetChoosableFileFilters();
                    return new NullTask();
                }
            }
        }
        File[] sf = jfc.getSelectedFiles();
        return new DocumentLoadThread(encoding, sf, true);
        } finally {
            jfc.setMultiSelectionEnabled(false);
        jfc.resetChoosableFileFilters();

        }
    }

    void onUndoableEditAdd() {
        UndoManager undoManager = SRLGUIApp.getApplication().undoManager;
        jButton10.setEnabled(undoManager.canUndo());
        jButton10.setToolTipText(undoManager.canUndo() ? undoManager.getUndoPresentationName() : "Cannot undo");
        jMenuItem15.setEnabled(undoManager.canUndo());
        jMenuItem15.setToolTipText(undoManager.canUndo() ? undoManager.getUndoPresentationName() : "Cannot undo");
        jButton11.setEnabled(undoManager.canRedo());
        jButton11.setToolTipText(undoManager.canRedo() ? undoManager.getRedoPresentationName() : "Cannot redo");
        jMenuItem16.setEnabled(undoManager.canRedo());
        jMenuItem16.setToolTipText(undoManager.canRedo() ? undoManager.getRedoPresentationName() : "Cannot redo");
    }

    @Action
    public void undo() {
        UndoManager undoManager = SRLGUIApp.getApplication().undoManager;
        if(undoManager.canUndo()) {
            undoManager.undo();
        } else {
            JOptionPane.showMessageDialog(this.getComponent(), "The last action cannot be undone. (Please file bug report)", "Undo Not Possible", JOptionPane.WARNING_MESSAGE);
        }
        SRLGUIApp.getApplication().proj.setModified();
        enableSave();
       onUndoableEditAdd();
    }

    @Action
    public void redo() {
        UndoManager undoManager = SRLGUIApp.getApplication().undoManager;
        if(undoManager.canRedo()) {
            undoManager.redo();
        } else {
            JOptionPane.showMessageDialog(this.getComponent(), "The last action cannot be undone. (Please file bug report)", "Undo Not Possible", JOptionPane.WARNING_MESSAGE);
        }
        SRLGUIApp.getApplication().proj.setModified();
        enableSave();
        onUndoableEditAdd();
    }

    @Action
    public void cut() {
    }

    @Action
    public void copy() {

    }

    @Action
    public void paste() {
    }

    @Action
    public void deleteRuleSet() {
        TreePath path = mainTree.getSelectionPath();
        if(path == null || path.getPath() == null || path.getPath().length != 3)
            return;
        Object o = ((DefaultMutableTreeNode)path.getPath()[1]).getUserObject();
        int ruleType;
        if(o.equals("Template Rules"))
            ruleType = Rule.TEMPLATE_RULE;
        else if(o.equals("Entity Rules"))
            ruleType = Rule.ENTITY_RULE;
        else
            return;
        String setName = (String)((DefaultMutableTreeNode)path.getPath()[2]).getUserObject();
        if(JOptionPane.showConfirmDialog(this.getFrame(), "Remove " + (ruleType == Rule.TEMPLATE_RULE ? "template" : "entity")
               + " rule set " + setName + "?", "Remove Rule Set", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
           return;
        List<RuleSet> rsList = ruleType == Rule.TEMPLATE_RULE ? SRLGUIApp.getApplication().proj.templateRulesets :
            SRLGUIApp.getApplication().proj.entityRulesets;
        RuleSet rs = null;
        for(RuleSet rs2 : rsList) {
            if(rs2.name.equals(setName))
                rs = rs2;
        }
        removeRuleSet(ruleType, setName, (DefaultMutableTreeNode)path.getPath()[2]);
        SRLGUIApp.getApplication().addUndoableEdit(new RemoveRuleSetEdit(setName, ruleType, rs));
    }

    @Action
    public void addRule() {
        Component c = rightPane.getSelectedComponent();
        if(c == null || !(c instanceof RuleSetPanel))
            return;
        RuleSetPanel panel = (RuleSetPanel)c;
        panel.addRule();
    }

    @Action
    public void removeRule() {
        Component c = rightPane.getSelectedComponent();
        if(c == null || !(c instanceof RuleSetPanel))
            return;
        RuleSetPanel panel = (RuleSetPanel)c;
        panel.removeRule();
    }

    @Action
    public void saveProjectAs() {
        SrlProject proj = SRLGUIApp.getApplication().proj;
        jfc.setSelectedFile(proj.getPath());
        if(jfc.showSaveDialog(getFrame()) != JFileChooser.APPROVE_OPTION)
            return;
        if (proj != null && proj.isModified()) {
            try {
                proj.writeProject(jfc.getSelectedFile());
                    SRLGUIApp.getApplication().clearModified();
            } catch (IOException x) {
                error(x, "Could not save project");
            } catch (CorpusConcurrencyException x) {
                error(x, "Could not save project");
            }
        }
    }

    @Action
    public void addWordListToSet() {
        Component c = rightPane.getSelectedComponent();
        if(c == null || !(c instanceof WordListPanel))
            return;
        WordListPanel panel = (WordListPanel)c;
        panel.addListAction();
    }

    @Action
    public void removeWordListFromSet() {
        Component c = rightPane.getSelectedComponent();
        if(c == null || !(c instanceof WordListPanel))
            return;
        WordListPanel panel = (WordListPanel)c;
        panel.deleteListAction();
    }

    @Action
    public void openLanguageDescription() {
         try {
            Desktop.getDesktop().browse(new URI("http://code.google.com/p/srl-editor/wiki/SRLLanguageDescription"));
        } catch(Exception x) {
            JOptionPane.showMessageDialog(this.getFrame(), x.getMessage(), "Could not open external browser", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class NullEdit extends SimpleUndoableEdit {
        public NullEdit() {}

        public String getPresentationName() {
            return "";
        }

        public void redo() throws CannotRedoException {
            throw new CannotRedoException();
        }

        public void undo() throws CannotUndoException {
            throw new CannotUndoException();
        }

        public boolean canRedo() {
            return false;
        }

        public boolean canUndo() {
            return false;
        }
    }

    private class AddRuleSetEdit extends SimpleUndoableEdit {
        String ruleSetName;
        int ruleType;
        DefaultMutableTreeNode dmtn;

        public AddRuleSetEdit(String ruleSetName, int ruleType, DefaultMutableTreeNode dmtn) {
            this.ruleSetName = ruleSetName;
            this.ruleType = ruleType;
            this.dmtn = dmtn;
        }

        public String getPresentationName() {
            return "Add rule set " + ruleSetName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            dmtn = addRuleSet(ruleSetName, ruleType, new RuleSet(ruleType, ruleSetName));
        }

        public void undo() throws CannotUndoException {
            undone = true;
            removeRuleSet(ruleType, ruleSetName, dmtn);
        }
    }

     private class ImportRuleSetEdit extends SimpleUndoableEdit {
        String ruleSetName;
        int ruleType;
        DefaultMutableTreeNode dmtn;
        RuleSet rs;

        public ImportRuleSetEdit(String ruleSetName, int ruleType, DefaultMutableTreeNode dmtn, RuleSet rs) {
            this.ruleSetName = ruleSetName;
            this.ruleType = ruleType;
            this.dmtn = dmtn;
            this.rs = rs;
        }


        public String getPresentationName() {
            return "Import rule set " + ruleSetName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            dmtn = addRuleSet(ruleSetName, ruleType, rs);
        }

        public void undo() throws CannotUndoException {
            undone = true;
            removeRuleSet(ruleType, ruleSetName, dmtn);
        }
    }

    private class RemoveRuleSetEdit extends SimpleUndoableEdit {
        String ruleSetName;
        int ruleType;
        DefaultMutableTreeNode node;
        RuleSet rs;

        public RemoveRuleSetEdit(String ruleSetName, int ruleType, RuleSet rs) {
            this.ruleSetName = ruleSetName;
            this.ruleType = ruleType;
            this.node = null;
            this.rs = rs;
        }

        public String getPresentationName() {
            return "Remove rule set " + ruleSetName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            removeRuleSet(ruleType, ruleSetName, node);
        }

        public void undo() throws CannotUndoException {
            undone = true;
            node = addRuleSet(ruleSetName, ruleType, rs);
        }
    }

    private class AddWordListSetEdit extends SimpleUndoableEdit {
        String wordListName;
        DefaultMutableTreeNode node;

        public AddWordListSetEdit(String wordListName, DefaultMutableTreeNode node) {
            this.wordListName = wordListName;
            this.node = node;
        }

        public String getPresentationName() {
            return "Add Word List Set " + wordListName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            node = addWordList(wordListName, new WordListSet(wordListName, SRLGUIApp.getApplication().proj.processor));
        }

        public void undo() throws CannotUndoException {
            undone = true;
            removeWordList(node);
        }
    }

    private class ImportWordListSetEdit extends SimpleUndoableEdit {
        String wordListName;
        DefaultMutableTreeNode node;
        WordListSet wls;

        public ImportWordListSetEdit(String wordListName, DefaultMutableTreeNode node, WordListSet wls) {
            this.wordListName = wordListName;
            this.node = node;
            this.wls = wls;
        }

        public String getPresentationName() {
            return "Add Word List Set " + wordListName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            node = addWordList(wordListName, wls);
        }

        public void undo() throws CannotUndoException {
            undone = true;
            removeWordList(node);
        }
    }

    private class RemoveWordListSetEdit extends SimpleUndoableEdit {
        String wordListName;
        DefaultMutableTreeNode node;
        WordListSet set;

        public RemoveWordListSetEdit(String wordListName, DefaultMutableTreeNode node, WordListSet set) {
            this.wordListName = wordListName;
            this.node = node;
            this.set = set;
        }


        public String getPresentationName() {
            return "Remove Word List Set " + wordListName;
        }

        public void redo() throws CannotRedoException {
            undone = false;
            removeWordList(node);
        }

        public void undo() throws CannotUndoException {
            undone = true;
            node = addWordList(wordListName, set);
        }


    }

    @Action
    public void openPlugInDialog() {
        PluginManagerDialog manager = new PluginManagerDialog();
        manager.setVisible(true);
    }

    @Action
    public void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this.getFrame(), true);
        dialog.setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem15;
    private javax.swing.JMenuItem jMenuItem16;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem20;
    private javax.swing.JMenuItem jMenuItem21;
    private javax.swing.JMenuItem jMenuItem22;
    private javax.swing.JMenuItem jMenuItem23;
    private javax.swing.JMenuItem jMenuItem24;
    private javax.swing.JMenuItem jMenuItem25;
    private javax.swing.JMenuItem jMenuItem26;
    private javax.swing.JMenuItem jMenuItem27;
    private javax.swing.JMenuItem jMenuItem28;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTree mainTree;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newProjectMenuItem;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTabbedPane rightPane;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private final Icon ruleSetIcon;
    private final Icon wordListIcon;
    private final Icon corpusIcon;
    private final Icon closeTabIcon;
    private final Icon searchIcon;
    private final Icon copyIcon;
    private final Icon cutIcon;
    private final Icon pasteIcon;
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
