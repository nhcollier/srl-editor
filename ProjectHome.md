# Introduction #
SRL (Simple rule language) is a regular expression based language developed to perform fact extraction from plain text. It is designed to operate as a two stage process by firstly extracting a set of predefined entity classes (named entity recognition) and then using these entities to fill in template slots using contextual rules. As an example, SRL is currently being used in the BioCaster project at the National Institute of Informatics for identifying disease outbreaks and their locations from news reports.

One of the key objectives of SRL has been to enable domain experts to develop their own lightweight text mining systems with minimal support from linguists and computer scientists. SRL uses a powerful yet easy-to-understand regular expression language designed specifically for handling tokenized text streams. SRL rules are often written using a simple combination of string literals and word classes (such as a list of country names) to define the matching context. If complexity is required the rules can easily incorporate constraints based on approximate matching, orthography, word distance etc.  The SRL Editor, written in platform independent Java, supports user design of SRL rule sets using a graphic user interface (GUI) aiming for rapid development and test cycles.

The SRL Editor aims to supply built in support for hand-crafted rule testing and revision, e.g. to find text segments where no rules are matching or to find rules which do not match any text. To enable fast creation and evaluation of rules the SRL editor allows the user to attach a corpus to the rule set, so that all matching contexts to a rule can be found rapidly and the user can instantly see if the rule is useful and correct. Future versions will aim to support improved rule revision mechanisms based on statistical metrics.

Once SRL rule sets have been developed it is possible to run these in command-line mode on un-indexed text collections.

# Downloads #

SRL is available for download here: [Version 1.0 Release Candidate 1](http://srl-editor.googlecode.com/files/srlgui-1.0rc1.zip)

# Documentation #
The SRL Manual is available [here](http://srl-editor.googlecode.com/files/srl-handbook.pdf)

A brief video introduction is available and can be downloaded [here](http://srl-editor.googlecode.com/files/SRL-Walkthrough.wmv). There is also a complete [description of the language](http://code.google.com/p/srl-editor/wiki/SRLLanguageDescription).

For those who wish to use SRL as a library within their own projects there is an [introduction](http://code.google.com/p/srl-editor/wiki/ProgrammingWithSRL) to the structure of the SRL library, a [code example](http://code.google.com/p/srl-editor/wiki/CodeExample1) and the Java-Doc [generated documentation](http://srl-editor.googlecode.com/files/javadoc.zip).

# Language Support #

Tokenization for languages other than English is supported through Lucene, including: Portuguese, Chinese, Czech, Dutch, French, German, Greek, Russian and we are working to include improved algorithms for Thai and Japanese.