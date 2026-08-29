# Legacy HSK vocabulary upload files

These files contain the exclusive vocabulary introduced at each level of the six-level HSK 2.0
system. They use the `POST /api/v1/words/chinese/bulk` request schema.
2,706 entries include compact, practical Simplified Chinese–English sentence pairs from Tatoeba.
Entries without a suitable reliable match have an empty `examples` list.

| File | Entries |
| --- | ---: |
| `hsk1.json` | 150 |
| `hsk2.json` | 147 |
| `hsk3.json` | 298 |
| `hsk4.json` | 598 |
| `hsk5.json` | 1,298 |
| `hsk6.json` | 2,500 |

The 4,991 records represent unique written entries. The traditional headline totals sometimes
quoted for HSK levels include nine entries that overlap or are represented by another form.

Upload a level while the API is running:

```bash
curl -X POST http://localhost:8080/api/v1/words/chinese/bulk \
  -H "Content-Type: application/json" \
  --data-binary @data/hsk/hsk1.json
```

## Source and license

Derived from [Complete HSK Vocabulary](https://github.com/drkameleon/complete-hsk-vocabulary),
using its exclusive legacy HSK word lists. The source provides simplified characters,
tone-marked pinyin, numeric-tone pronunciation, and parts of speech. Source part-of-speech
codes are converted to readable values in the `wordTypes` array. It is distributed under the MIT
License:

Copyright (c) 2026 Yanis Zafirópulos (aka Dr.Kameleon)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Dictionary source and license

English translation senses for 4,988 of the 4,991 entries are taken from
[CC-CEDICT](https://cc-cedict.org/wiki/), published by MDBG under the Creative Commons
Attribution-ShareAlike 4.0 International license. The three HSK phrases without a CC-CEDICT
headword match retain the meanings from Complete HSK Vocabulary. Run
`scripts/import-cc-cedict.py` with the official MDBG gzip release to reproduce the import.

## Example sentence source

Example sentences are selected from the Chinese–English portion of the
[OPUS Tatoeba corpus](https://opus.nlpl.eu/datasets/Tatoeba), release `v2026-07-08`, distributed
under CC BY 2.0 FR. Selection uses jieba's word-frequency dictionary to prevent substring matches,
rejects ambiguous single-character matches, and uses OpenCC's mapping to keep HSK examples in
Simplified Chinese. OPUS requests citation of Jörg Tiedemann, “Parallel Data, Tools and Interfaces,”
LREC 2012.
