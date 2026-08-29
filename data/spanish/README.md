# Spanish vocabulary upload files

These are practical, frequency-banded Spanish vocabulary lists—not official CEFR inventories.
CEFR describes language ability and does not define a canonical exhaustive vocabulary for each
level.
8,789 entries include compact, practical Spanish–English Tatoeba sentence pairs selected using the
source's lemma annotations. Entries without a suitable reliable match have an empty `examples` list.

| File | Entries | Frequency band |
| --- | ---: | ---: |
| `a1.json` | 500 | 1–500 |
| `a2.json` | 1,000 | 501–1,500 |
| `b1.json` | 1,500 | 1,501–3,000 |
| `b2.json` | 2,000 | 3,001–5,000 |
| `c1.json` | 2,500 | 5,001–7,500 |
| `c2.json` | 3,000 | 7,501–10,500 |

The files use the `POST /api/v1/words/spanish/bulk` request schema. Spanish spelling is used in
the required `pronunciation` field; browser speech synthesis can provide spoken pronunciation.

```bash
curl -X POST http://localhost:8000/api/v1/words/spanish/bulk \
  -H "Content-Type: application/json" \
  --data-binary @data/spanish/a1.json
```

## Source and method

Generated from [doozan/spanish_data](https://github.com/doozan/spanish_data), using its Spanish
frequency list, Spanish–English Wiktionary data, and part-of-speech metadata. Source lemmas are
assigned to CEFR-inspired bands in descending frequency order. This is an estimate intended for
curriculum seeding, not an Instituto Cervantes or Council of Europe standard.

The source repository is distributed under CC BY 4.0 and attributes underlying Wiktionary and
frequency data as described in its README. Changes made here include filtering to unique alphabetic
lemmas, choosing the matching part-of-speech definition, converting POS labels, and assigning
frequency bands.

Examples come from `sentences.tsv` in the same source repository. Those sentences originate from
[Tatoeba](https://tatoeba.org/) and are distributed under CC BY 2.0 FR; the source file retains the
sentence IDs and contributor attribution. The importer prioritizes short, highly reviewed pairs and
supports inflected forms through the supplied lemma annotations.
