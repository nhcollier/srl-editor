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

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import java.util.*;
import java.io.*;
import javax.swing.JOptionPane;
import javax.swing.tree.*;
import srl.corpus.CorpusConcurrencyException;
import srl.project.SrlProject;
import srl.rule.*;
import srl.wordlist.*;

/**
 * The main class of the application.
 */
public class SRLGUIApp extends SingleFrameApplication {

    HashMap<String,RuleSet> entityRuleSets;
    HashMap<String,RuleSet> templateRuleSets;
    HashMap<String,WordList> wordLists;
    public SrlProject proj;
    
    public static final int SRL_ENTITY_RULESET = 1;
    public static final int SRL_TEMPLATE_RULESET = 2;
    public static final int SRL_WORDLIST = 3;
    public static final int SRL_CORPUS = 4;
    public static final int SRL_PROJECT = 5;
    public static final int SRL_SEARCH = 6;
    public static final int SRL_SHOW_DOC = 7;
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        entityRuleSets = new HashMap<String,RuleSet>();
        templateRuleSets = new HashMap<String, RuleSet>();                
        wordLists = new HashMap<String,WordList>();
        show(new SRLGUIView(this));
        ExitListener exitListen = new ExitListener() {

            public boolean canExit(EventObject arg0) {
                if(proj != null && proj.isModified()) {
                    int opt = JOptionPane.showConfirmDialog(SRLGUIApp.this.getMainFrame(), 
                            "Project modified, save before closing?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
                    if(opt == JOptionPane.YES_OPTION) {
                        try {
                            proj.writeProject();
                    SRLGUIApp.getApplication().clearModified();
                        } catch(IOException x) {
                            x.printStackTrace();
                            JOptionPane.showMessageDialog(SRLGUIApp.this.getMainFrame(), 
                                    x.getMessage(), "Could not save project", JOptionPane.ERROR_MESSAGE);
                            return false;
                        } catch(CorpusConcurrencyException x) {
                            x.printStackTrace();
                            JOptionPane.showMessageDialog(SRLGUIApp.this.getMainFrame(), 
                                    x.getMessage(), "Could not save project", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                        return true;
                    } else if(opt == JOptionPane.NO_OPTION) {
                        return true;
                    } else if(opt == JOptionPane.CANCEL_OPTION) {
                        return false;
                    }
                }
                return true;
            }

            public void willExit(EventObject arg0) {
                
            }
        };
        addExitListener(exitListen);
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        if(proj != null) {
            try {
                proj.corpus.closeCorpus();
            } catch(IOException x) {
                x.printStackTrace();
            }
        }
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    DefaultMutableTreeNode mainTreeNode;
    DefaultMutableTreeNode entityRules;
    DefaultMutableTreeNode templateRules;
    DefaultMutableTreeNode wordList;
    DefaultMutableTreeNode corpus;
    
    
    public MutableTreeNode getMainTreeNode() {
        if(mainTreeNode == null) {
            mainTreeNode = new DefaultMutableTreeNode("SRL Elements");
            entityRules = new DefaultMutableTreeNode("Entity Rules");
            mainTreeNode.add(entityRules);
            templateRules = new DefaultMutableTreeNode("Template Rules");
            mainTreeNode.add(templateRules);
            
            wordList = new DefaultMutableTreeNode("Word List Sets");
            mainTreeNode.add(wordList);
            if(proj != null) {
                for(WordList wl : proj.wordlists) {
                    wordList.add(new DefaultMutableTreeNode(wl.name));
                }
                for(RuleSet rs : proj.entityRulesets) {
                    entityRules.add(new DefaultMutableTreeNode(rs.name));
                }
                for(RuleSet rs: proj.templateRulesets) {
                    templateRules.add(new DefaultMutableTreeNode(rs.name));
                }
            }
            corpus = new DefaultMutableTreeNode("Corpus");
            mainTreeNode.add(corpus);
        }
        
        return mainTreeNode;
    }
    
    public void closeTab(int tabType, String tabName) {
        ((SRLGUIView)getMainView()).closeTab(tabType, tabName);
    }
    
    public void setModified() {
        proj.setModified();
        SRLGUIView view = (SRLGUIView)getMainView();
        view.enableSave();
    }
    
    public void clearModified() {
        ((SRLGUIView)getMainView()).disableSave();
    }
    
    /**
     * A convenient static getter for the application instance.
     * @return the instance of SRLGUIApp
     */
    public static SRLGUIApp getApplication() {
        return Application.getInstance(SRLGUIApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(SRLGUIApp.class, args);
    }
}
