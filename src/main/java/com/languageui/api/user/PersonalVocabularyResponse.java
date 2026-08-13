package com.languageui.api.user;

import java.util.UUID;
import java.util.List;

public record PersonalVocabularyResponse(
        UUID id,
        String language,
        String status,
        String term,
        String translation,
        String pronunciation,
        String pinyin,
        String level,
        List<String> wordTypes) {
}
