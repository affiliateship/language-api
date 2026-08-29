#!/usr/bin/env python3
"""Apply CC-CEDICT definitions and the list-based catalog schema.

Usage: import-cc-cedict.py CEDICT_GZIP

CC-CEDICT is downloaded from MDBG and licensed under CC BY-SA 4.0.
"""

import gzip
import json
import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
ENTRY_RE = re.compile(r"^(\S+) (\S+) \[([^]]+)] /(.*)/$")
UNSUITABLE_EXAMPLE = re.compile(
    r"\b(kill(?:ed|ing)?|murder|suicide|dead|death|gun|war|president|government|"
    r"sex|naked|drunk|beer|wine|whisk(?:e)?y|half[ -]sister|fall in love|paranormal|blood)\b",
    re.IGNORECASE,
)


def normalized_pinyin(value):
    return " ".join(value.casefold().replace("u:", "v").split())


def load_cedict(path):
    exact = {}
    by_word = {}
    with gzip.open(path, "rt", encoding="utf-8") as source:
        for line in source:
            if line.startswith("#"):
                continue
            match = ENTRY_RE.match(line.rstrip())
            if not match:
                continue
            _traditional, simplified, pinyin, definition_text = match.groups()
            definitions = [value.strip() for value in definition_text.split("/") if value.strip()]
            key = (simplified, normalized_pinyin(pinyin))
            exact.setdefault(key, []).extend(definitions)
            by_word.setdefault(simplified, []).extend(definitions)
    return exact, by_word


def unique(values, limit=30):
    result = []
    seen = set()
    for value in values:
        key = value.casefold()
        if key not in seen:
            result.append(value)
            seen.add(key)
        if len(result) == limit:
            break
    return result


def split_existing(value):
    if isinstance(value, list):
        return unique(value)
    return unique(part.strip() for part in value.split(";") if part.strip())


def practical_examples(entry):
    if "examples" in entry:
        candidates = entry["examples"]
    else:
        text = entry.pop("example", "").strip()
        translation = entry.pop("exampleTranslation", "").strip()
        candidates = ([{"text": text, "englishTranslation": translation}]
                      if text and translation else [])
    result = []
    for example in candidates:
        text = example["text"].strip()
        translation = example["englishTranslation"].strip()
        # Definition placeholders and long or unsuitable sentences are poor study prompts.
        if "可以表示" in text or "puede significar" in text:
            continue
        if len(text) > 100 or len(translation) > 140 or UNSUITABLE_EXAMPLE.search(translation):
            continue
        result.append({"text": text, "englishTranslation": translation})
    return result[:10]


def transform_file(path, exact, by_word, chinese):
    payload = json.loads(path.read_text(encoding="utf-8"))
    matched = 0
    for entry in payload["words"]:
        existing = split_existing(entry["englishTranslation"])
        if chinese:
            key = (entry["word"], normalized_pinyin(entry["pronunciation"]))
            definitions = exact.get(key) or by_word.get(entry["word"])
            if definitions:
                entry["englishTranslation"] = unique(definitions)
                matched += 1
            else:
                entry["englishTranslation"] = existing
        else:
            entry["englishTranslation"] = existing
        entry["examples"] = practical_examples(entry)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return matched, len(payload["words"])


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    exact, by_word = load_cedict(sys.argv[1])
    matched = total = 0
    for path in sorted((ROOT / "data" / "hsk").glob("hsk*.json")):
        file_matched, file_total = transform_file(path, exact, by_word, True)
        matched += file_matched
        total += file_total
    for path in sorted((ROOT / "data" / "spanish").glob("*.json")):
        transform_file(path, exact, by_word, False)
    print(f"Applied CC-CEDICT definitions to {matched}/{total} Chinese entries")


if __name__ == "__main__":
    main()
