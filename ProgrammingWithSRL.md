# Introduction #
The SRL files can also be used as a programming with other applications. This guide shows the main classes and functions used. The full javadoc can be found here.

# Projects #
SRL organises the files it uses into "projects" these projects consist of the following elements
  * Entity Rules: These are used for extracting entities from text
  * Template Rules: These are used for filling slots once the entity rules have been applied
  * Word lists: These are used to organize words for rules
  * Corpus: This is a sequence of documents which can be matched to

There are two main methods for creating an SRL Project, firstly the constructor can be called to create a new **empty** project. This takes three parameters a **directory** for the project to be placed into which must be new or empty, a processor object (see below) and an optional parameter specifying whether a corpus should be created, if you set this to false the corpus object will be none. Elements from the project can be specified through the following fields.
  * `entityRulesets`
  * `templateRulesets`
  * `wordlists`
  * `corpus`
In addition the following fields can also be set
  * name: The name of the project (documentation use only)
  * description: The description of the project (documentation use only)
There is also a modified flag that can be set by `setModified()` and is unset by `writeProject()`. **If you change the fields you need to call setModified()**.

# Processor #
The processor describes all the linguistic processing features, this can be created by calling the constructor. For languages supported by this can be constructed by passing the language name. Alternatively you can provide custom splitters, tokenizers and analyzers (see JavaDoc and Lucene documentation)
The supported languages are
  * English
  * Brazilian
  * Chinese
  * CJK
  * Czech
  * Dutch
  * French
  * German
  * Greek
  * Russian
  * Thai
  * Japanese
(Here Brazilian means Brazilian Portugese and CJK is generic support for Chinese, Japanese and Korean, which tokenizes every character as a single word)

# Rules #
Rules are organised into rule sets, individual rules can be accessed through the `rules` field. The key function for matching rules is `getMatch(SrlDocument,boolean)`. This requires an instantiated `SrlDocument` object and a second parameter turns on and off multiple matching. This returns the matches to each variable in the rule. The function `getHeads` applies the template rule and outputs the result in standard form.

# Word lists #
Word list are stored as sets of wordlists. Each set has an identifier and each list has itself a unique identifier. A word list can be found directly by calling the static function `getWordList`. A wordlist is then returned as a `WordList` object, which is a list of `WordListEntry`s.

# Corpus #
Corpus is a wrapper for Lucene and contains many functions. Any particular Document can be manipulated by `addDoc` and `removeDoc` and obtained by `getDoc`. Once a document is added it is stored in the Lucene database as multiple fields, a root document and for each sentence a sentence entry. A root document has the following fields
  * name: The document name (this is a sequence of alphanumeric characters)
  * uid: A Unique identifier for this database entry
  * originalContents: The full unmodified contents of the document
  * sentCount: _Partially implemented feature, please ignore_
Each sentence record has the following fields
  * name: The document name, a single whitespace then the sentence number
  * uid: A unique identifier for this database entry
  * contents: The tokenized and lowercase version of the sentence
  * taggedContents: If corpus tagging has been applied this represents the tags
  * extracted: If template extraction has been applied the extracted templates.
For more on corpus see the API documentation.