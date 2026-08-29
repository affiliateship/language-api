def word_type:
  {
    "a": "adjective",
    "ad": "adverbial adjective",
    "an": "nominal adjective",
    "b": "non-predicate adjective",
    "c": "conjunction",
    "cc": "coordinating conjunction",
    "d": "adverb",
    "e": "interjection",
    "f": "direction or location word",
    "g": "morpheme",
    "k": "suffix",
    "l": "fixed expression",
    "m": "numeral",
    "Mg": "numeral morpheme",
    "mq": "numeral-classifier phrase",
    "n": "noun",
    "nr": "personal name",
    "ns": "place name",
    "nt": "organization name",
    "nz": "proper noun",
    "o": "onomatopoeia",
    "p": "preposition",
    "q": "classifier",
    "qt": "temporal classifier",
    "qv": "verbal classifier",
    "r": "pronoun",
    "s": "space word",
    "t": "time word",
    "tg": "time-word morpheme",
    "u": "auxiliary",
    "v": "verb",
    "vn": "nominal verb",
    "y": "modal particle",
    "z": "descriptive word"
  }[.] // .;

def primary_gloss:
  split(";")[0]
  | sub("^(\\([^)]*\\)\\s*)+"; "")
  | sub("[.。]$"; "");

{
  words: map({
    word: .simplified,
    englishTranslation: .forms[0].meanings,
    pronunciation: .forms[0].transcriptions.numeric,
    pinyin: .forms[0].transcriptions.pinyin,
    level: $level,
    wordTypes: ([.pos[]? | word_type] | unique | if length == 0 then ["unclassified"] else . end),
    examples: []
  })
}
