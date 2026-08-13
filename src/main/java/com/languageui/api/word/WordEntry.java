package com.languageui.api.word;

import java.util.UUID;
import java.util.List;

public record WordEntry(
        UUID id,
        String language,
        String word,
        String englishTranslation,
        String pronunciation,
        String pinyin,
        String level,
        List<String> wordTypes,
        String example,
        String exampleTranslation) {
}
