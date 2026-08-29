#!/usr/bin/env python3
"""Import real Tatoeba examples into the packaged vocabulary JSON files.

Usage:
    import-vocabulary-examples.py SPANISH_SENTENCES_TSV CHINESE_SENTENCES ENGLISH_SENTENCES JIEBA_DICT OPENCC_ST

The Spanish input is doozan/spanish_data's sentences.tsv, which includes lemma annotations.
The Chinese and English inputs are aligned plain-text files from the OPUS Tatoeba corpus.
"""

import csv
import json
import pathlib
import re
import sys
import math


ROOT = pathlib.Path(__file__).resolve().parents[1]
WORD_RE = re.compile(r"[^\W\d_]+", re.UNICODE)
UNSUITABLE_EXAMPLE = re.compile(
    r"\b(kill(?:ed|ing)?|murder|suicide|dead|death|gun|war|president|government|"
    r"sex|naked|drunk|beer|wine|whisk(?:e)?y|half[ -]sister|fall in love|paranormal|blood)\b",
    re.IGNORECASE,
)


def load_entries(directory):
    files = {}
    entries = {}
    for path in sorted(directory.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        files[path] = payload
        for entry in payload["words"]:
            entries.setdefault(entry["word"], []).append(entry)
    return files, entries


def candidate_score(source, translation, quality=0):
    """Prefer reviewed, compact sentences that still provide enough context."""
    length_penalty = abs(len(source) - 28) + abs(len(translation) - 45)
    return quality * 1000 - length_penalty


def is_practical(source, translation):
    return (5 <= len(source) <= 100 and 5 <= len(translation) <= 140
            and not UNSUITABLE_EXAMPLE.search(translation))


def spanish_lemmas(tags):
    lemmas = set()
    for tag in tags.split():
        if not tag.startswith(":"):
            continue
        for value in tag[1:].split(",")[1:]:
            lemmas.update(part for part in value.split("|") if part)
    return lemmas


def import_spanish(path, wanted):
    best = {}
    with pathlib.Path(path).open(encoding="utf-8", newline="") as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) < 6:
                continue
            english, spanish, _attribution, english_quality, spanish_quality, tags = row[:6]
            if not is_practical(spanish, english):
                continue
            lemmas = spanish_lemmas(tags) & wanted
            if not lemmas:
                # The annotations occasionally omit an otherwise exact headword.
                lemmas = set(WORD_RE.findall(spanish.casefold())) & wanted
            quality = min(int(english_quality), int(spanish_quality))
            score = candidate_score(spanish, english, quality)
            for lemma in lemmas:
                if lemma not in best or score > best[lemma][0]:
                    best[lemma] = (score, spanish, english)
    return best


def load_chinese_dictionary(path):
    frequencies = {}
    with pathlib.Path(path).open(encoding="utf-8") as source:
        for line in source:
            word, frequency, _part_of_speech = line.rstrip().split(" ", 2)
            frequencies[word] = int(frequency)
    return frequencies, sum(frequencies.values()), max(map(len, frequencies))


def traditional_characters(path):
    characters = set()
    with pathlib.Path(path).open(encoding="utf-8") as source:
        for line in source:
            if line.startswith("#") or not line.strip():
                continue
            simplified, traditional = line.rstrip().split("\t", 1)
            characters.update(value for value in traditional.split() if value != simplified)
    return characters


def segment(sentence, frequencies, total, maximum_length):
    """Maximum-probability segmentation using jieba's word-frequency dictionary."""
    route = [(0.0, len(sentence))] * (len(sentence) + 1)
    route[-1] = (0.0, len(sentence))
    log_total = math.log(total)
    for start in range(len(sentence) - 1, -1, -1):
        choices = []
        for end in range(start + 1, min(len(sentence), start + maximum_length) + 1):
            word = sentence[start:end]
            frequency = frequencies.get(word)
            if frequency:
                choices.append((math.log(frequency) - log_total + route[end][0], end))
        route[start] = max(choices or [(route[start + 1][0] - log_total, start + 1)])
    words = []
    cursor = 0
    while cursor < len(sentence):
        end = route[cursor][1]
        words.append(sentence[cursor:end])
        cursor = end
    return words


def import_chinese(chinese_path, english_path, dictionary_path, opencc_path, wanted):
    best = {}
    frequencies, total, maximum_length = load_chinese_dictionary(dictionary_path)
    traditional = traditional_characters(opencc_path)
    with pathlib.Path(chinese_path).open(encoding="utf-8") as chinese_source, \
            pathlib.Path(english_path).open(encoding="utf-8") as english_source:
        for chinese, english in zip(chinese_source, english_source, strict=True):
            chinese, english = chinese.strip(), english.strip()
            if not is_practical(chinese, english):
                continue
            if any(character in traditional for character in chinese):
                continue
            score = candidate_score(chinese, english)
            # Single-character substring matches are too ambiguous to import safely.
            for word in set(segment(chinese, frequencies, total, maximum_length)) & wanted:
                if len(word) == 1:
                    continue
                if word not in best or score > best[word][0]:
                    best[word] = (score, chinese, english)
    return best


def apply(examples, entries):
    updated = 0
    for word, (_score, example, translation) in examples.items():
        for entry in entries[word]:
            entry.setdefault("examples", []).append({
                "text": example,
                "englishTranslation": translation,
            })
            updated += 1
    return updated


def reset_fallbacks(entries, language):
    for word_entries in entries.values():
        for entry in word_entries:
            entry["examples"] = []


def save(files):
    for path, payload in files.items():
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    if len(sys.argv) != 6:
        raise SystemExit(__doc__)
    spanish_files, spanish_entries = load_entries(ROOT / "data" / "spanish")
    chinese_files, chinese_entries = load_entries(ROOT / "data" / "hsk")
    spanish = import_spanish(sys.argv[1], set(spanish_entries))
    chinese = import_chinese(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], set(chinese_entries))
    reset_fallbacks(spanish_entries, "Spanish")
    reset_fallbacks(chinese_entries, "Chinese")
    spanish_count = apply(spanish, spanish_entries)
    chinese_count = apply(chinese, chinese_entries)
    save(spanish_files)
    save(chinese_files)
    print(f"Imported {spanish_count}/{sum(map(len, spanish_entries.values()))} Spanish examples")
    print(f"Imported {chinese_count}/{sum(map(len, chinese_entries.values()))} Chinese examples")


if __name__ == "__main__":
    main()
