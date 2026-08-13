#!/usr/bin/env python3
"""Generate frequency-banded Spanish vocabulary upload files.

Usage: generate-spanish-word-files.py FREQUENCY_CSV DICTIONARY_DATA OUTPUT_DIRECTORY
"""

import csv
import json
import pathlib
import sys

LEVEL_SIZES = {"A1": 500, "A2": 1000, "B1": 1500, "B2": 2000, "C1": 2500, "C2": 3000}
POS_NAMES = {
    "adj": "adjective", "adv": "adverb", "art": "article", "conj": "conjunction",
    "det": "determiner", "determiner": "determiner", "interj": "interjection",
    "letter": "letter", "n": "noun", "num": "numeral", "particle": "particle",
    "prep": "preposition", "pron": "pronoun", "prop": "proper noun", "v": "verb",
}


def load_dictionary(path):
    entries = {}
    for block in pathlib.Path(path).read_text(encoding="utf-8").split("_____\n"):
        lines = block.splitlines()
        if not lines:
            continue
        word = lines[0].strip()
        current_pos = "unclassified"
        for line in lines[1:]:
            value = line.strip()
            if value.startswith("pos: "):
                current_pos = value[5:].strip()
            elif value.startswith("gloss: "):
                entries.setdefault(word, []).append((current_pos, value[7:].strip()))
    return entries


def select_definition(definitions, preferred_pos):
    preferred = [item for item in definitions if item[0] == preferred_pos]
    pos, definition = (preferred or definitions)[0]
    return definition[:1000], POS_NAMES.get(pos, pos.replace("_", " "))


def main():
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    frequency_path, dictionary_path, output_path = sys.argv[1:]
    dictionary = load_dictionary(dictionary_path)
    words = []
    seen = set()
    with open(frequency_path, encoding="utf-8", newline="") as source:
        for row in csv.DictReader(source):
            word = row["spanish"].strip()
            if word in seen or len(word) > 200 or not word.replace("-", "").isalpha():
                continue
            definitions = dictionary.get(word)
            if not definitions:
                continue
            translation, word_type = select_definition(definitions, row["pos"])
            words.append((word, translation, word_type))
            seen.add(word)
            if len(words) == sum(LEVEL_SIZES.values()):
                break

    destination = pathlib.Path(output_path)
    destination.mkdir(parents=True, exist_ok=True)
    offset = 0
    for level, size in LEVEL_SIZES.items():
        level_words = words[offset:offset + size]
        payload = {"words": [{
            "word": word,
            "englishTranslation": translation,
            "pronunciation": word,
            "pinyin": None,
            "level": level,
            "wordTypes": [word_type],
            "example": f"Estoy aprendiendo la palabra «{word}».",
            "exampleTranslation": f"I am learning the word “{word}”.",
        } for word, translation, word_type in level_words]}
        (destination / f"{level.lower()}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        offset += size


if __name__ == "__main__":
    main()
