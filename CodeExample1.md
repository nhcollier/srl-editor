
```
import srl.corpus.*;
import srl.project.*;
import srl.rule.*;
import srl.wordlist.*;
import java.util.*;
import java.io.*;
import srl.tools.struct.Pair;


public class CodeExample1 {
    public static void main(String[] args) {
	try {
	    // Instantiate a new project with English language processing
	    SrlProject project = new SrlProject(new File("codeexample1"),
						Processor.getProcessor("English"));
	    
	    // Add a document to the corpus
	    project.corpus.addDoc("testDocName", 
				  "This is the contents of the test document", 
				  true);
	    
	    // Create a rule set
	    RuleSet ruleSet = new RuleSet(Rule.ENTITY_RULE, "testRuleSet");
	    project.entityRulesets.add(ruleSet);
	    
	    // If you modify the project you should change the flag
	    project.setModified();
	    
	    // Add a entity type
	    project.entities.add(new Pair<String,String>("name","entity"));
	    
	    // Add a rule to it 
	    Rule entityRule = Rule.ruleFromString(":- \"test\" name(entity,T)",
						  Rule.ENTITY_RULE);
	    ruleSet.rules.add(new Pair<String,Rule>("RULEID",
						    entityRule));
	    
	    // Add a word list
	    WordListSet wordListSet = new WordListSet("wordListSetName", 
						      project.processor);
	    project.wordlists.add(wordListSet);
	    wordListSet.addList("wordListID");
	    String[] wordListEntries = { "some", "words", "in", "this", "list" };
	    WordListSet.addToList("wordListID",wordListEntries);
	    
	    // Create a corpus extractor object
	    CorpusExtractor extractor = new CorpusExtractor(project.corpus);

	    // Apply matching to corpus documents
	    // 2nd parameter allows for overlap detection
	    extractor.tagCorpus(project.entityRulesets,null,true);
	    
	    // Get the result
	    List<String> results = project.corpus.getDocTaggedContents("testDocName");
	    for(String result : results) {
		System.out.println(result);
	    }
	    
	    // Apply template rules
	    extractor.extractTemplates(project.templateRulesets,true);
	    
	    // Get the result
	    results = project.corpus.getDocTemplateExtractions("testDocName");
	    
	    // Save the project
	    project.writeProject();
	} catch(Exception x) {
	    x.printStackTrace();
	    System.exit(-1);
	}
    }
}
```