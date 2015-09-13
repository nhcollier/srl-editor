# SRL Rule Language #

## Rules: ##
SRL consists of two types of rules entity and template rules. Entity rules are used for detecting named entities in text, then they simply add XML tags to the texts. Template rules are applied after entity rules and are used for completing slots, for example a rule to detect the perpetrator of a crime would look like perpetrator(P) :- ... name(person,P) ... The primary syntactic difference between entity rules and template rules is that template rules have a head that specifies the slots to be filled.
It is important to note that the text is first tokenized into words and that most operators in the rule apply to only a single token.

### Rule language: ###
```
entity_rule := ":-" <body_expression>
template_rule := <head_expression> ":-" <body_expression>
head_expression := <head> ( ";" <head> )*
head := <id> "(" <var> ")"
body_expression := ( "list" "(" <wordlist> ")" |
		     "ortho" "(" <literal> ")" |
		     "words" "(" <number>? "," <number>? ")" |
		     <literal> |
                     "begins" "(" <literal> ")" |
                     "ends" "(" <literal> ")" |
                     "contains" "(" <literal> ")" |
		     "regex" "(" <literal> ")" |
                     "optional" "(" <literal> ")" |
                     "not" "(" <literal> | <wordlist> ")" |
                     "case" "(" <literal> ")" |
	             <entity_expression> )*
entity_expression := <id> "*"? "(" <id> ( "," <var> )? ")" ( "{" <body_expression>† "}" )?
<id> := A sequence of alphanumeric characters starting with a lowercase letter
<var> := A sequence of alphanumeric characters starting with an uppercase letter
<wordlist> := A "@" or "%" followed by alphanumeric characters
<literal> := Any string enclosed by quotation marks
```
_† An entity expression's body is not allowed to contain another entity expression_
### Example: ###

`disease(D) :- “tested” “positive” “for” name(disease,D) { strmatches(@disease) } `

This is a template rule matching any sequence starting with “tested positive for” and then a named entity from the list of diseases `@disease`, outputting the matching result as filling the slot `disease(...)`. The entity rule to find an entity from the same pattern would look like

` :- "tested" "positive" "for" name(disease,D) { strmatches(@disease) } `
## list: ##
Matches a sequence of words from a word list or word list set. A word list identifier begins with "@" for example `@wordlist`. Alternatively the system can match to any word list in a particular set, this is done by using the word list set name with a "%" for example `%wordlistset`.

E.g., `list(@wordlist)` where `@wordlist=(“cat”,”dog”,”fire breathing dragon”)` matches all three either “cat”, “dog” or “fire” “breathing” “dragon” (3 tokens).
## words: ##
Matches a sequence of words. ` words(x,y) ` matches between _x_ and _y_ words. If _x_ is omitted it is taken to zero, if _y_ is omitted it is taken to be infinity

e.g, `words(1,3)`: Matches one to three tokens

`words(,3)`: Matches up to three tokens

`words(1,)`: Matches at least one token

## Literals: ##
A literal matches only exactly its value, it must be enclosed in quotation marks. Please ensure that all literals correspond to a single token, for example “fire breathing dragons” will never match and should be formatted as “fire” “breathing” “dragons”
## Entities: ##
Entities are defined as `entityType(entityVal,var) { body } `. An entity matches if its body matches. The body may be omitted for entity rules an omitted body is equivalent to ` { words(1,1) } ` for template rules it is equivalent to ` { words(1,) } `.

e.g., ` name(disease,D) ` matches “`<name cl=”disease”>dengue fever</name>`” in a template rule, variable D is then bound to “dengue fever” for outputting in the head.

` time(date,D) { strmatches(@month) ortho(“2N”) ortho(“4N”) } ` matches “April 17 1984” and tags it as “`<time cl=”date”>April 17 1984</name>`”

If the variable is not needed (i.e., the entity does not match to any heads), it may be omitted (the system will replace it with an automatic variable of the form `EXPR##`).

The special form `entityType*(entityVal,var) { body } ` is equivalent to the form
`entityType(entityVal,var) { words(,) body words(,) } `. This means the entity contains the given body but is not limited. This feature can make template rules more concise.

## begins,ends,contains: ##
`begins`,`ends`,`contains` match to a single word which begin, end or
contain respectively the chosen literal

E.g. `begins("work")` matches "work", "works, "worked" and "working"

## optional: ##
`optional` matches a single literal, or skips the match if the literal is not there

E.g. `optional("tabby") "cat"` matches both "tabby cat" and "cat"

## not: ##
`not`} matches as long as the given literal or word list is not matched.

E.g. `not("tabby") "cat"` matches "cat" but not "tabby cat"

## case: ##
`case` matches a case-sensitive literal

E.g. `case("SRL")` matches "SRL" but not "Srl"

## ortho: ##
Ortho provides simple orthographic forms
```
ortho := ortho_form (“&” ortho_form)*
ortho_form := “^”? <number>? “+”? <ortho_class>
```
Ortho classes are the same as Unicode categories
  * L: any kind of letter from any language.
  * Ll: a lowercase letter that has an uppercase variant.
  * Lu: an uppercase letter that has a lowercase variant.
  * Lt: a letter that appears at the start of a word when only the first letter of the word is capitalized.
  * L&: a letter that exists in lowercase and uppercase variants (combination of Ll, Lu and Lt).
  * Lo: a letter or ideograph that does not have lowercase and uppercase variants.
  * S: math symbols, currency signs, dingbats, box-drawing characters, etc..
  * Sm: any mathematical symbol.
  * Sc: any currency sign.
  * Sk: a combining character (mark) as a full character on its own.
  * So: various symbols that are not math symbols, currency signs, or combining characters.
  * N: any kind of numeric character in any script.
  * Nd: a digit zero through nine in any script except ideographic scripts.
  * Nl: a number that looks like a letter, such as a Roman numeral.
  * No: a superscript or subscript digit, or a number that is not a digit 0..9 (excluding numbers from ideographic scripts).
  * Np: numbers and or comma and period.
  * P: any kind of punctuation character.
  * Pd: any kind of hyphen or dash.
  * Ps: any kind of opening bracket.
  * Pe: any kind of closing bracket.
  * Pi: any kind of opening quote.
  * Pf: any kind of closing quote.
  * Pc: a punctuation character such as an underscore that connects words.
  * Po: any kind of punctuation character that is not a dash, bracket, quote or connector.
  * In\_Block_: a character from a specific Unicode block
  * InBasicLatin: An ASCII character
  * InHiragana
  * InKatakana
  * InCJKUnifiedIdeographs: Chinese symbols (漢字)
  * InThai
These are prefixed by
  * Nothing: The string is entirely composed of this type of character
  * ^: The string starts with this character type
  *_x_: The string is exactly_x_characters of this type
  *_x_+: The string contains at least_x_characters of the chosen type_

E.g., `ortho(“^Lu”)`: Matches a single token starting with an upper-case letter

`ortho(“4Nd”)`: Matches a single token consisting of exactly four digits like “2008”

`ortho(“^Lu&1+Ll”)`: Matches a single token starting with an upper-case letter and containing at least one lower-case letter

`ortho(“InThai”)`: Matches a single token composed of solely Thai letters
For more details on Unicode categories see [Unicode.org](http://www.unicode.org/)

## regex: ##
Matches a single token according to a regular expression, see [Sun Java Documentation](http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html)

e.g., `regex(“.*virus”)` matches “coronavirus”

## Other notes ##
Please avoid rules do not have literals, ` strmatches ` or entities (only for template rules). These can be significantly slower to search as matching sentences/documents can not be found quickly by the search algorithm. Searching speed can be vastly improved by including more literals. Entities are only indexed for template rules, so including entities does not improve the performance of entity rules.