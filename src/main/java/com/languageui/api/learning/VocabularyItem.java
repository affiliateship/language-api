package com.languageui.api.learning;

import java.util.UUID;

public record VocabularyItem(UUID id, UUID lessonId, String term, String translation, String pronunciation) {
}
