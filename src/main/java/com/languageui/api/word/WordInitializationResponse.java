package com.languageui.api.word;

public record WordInitializationResponse(
        int inserted,
        int skipped,
        int insertedChineseWords,
        int skippedChineseWords,
        int insertedSpanishWords,
        int skippedSpanishWords,
        long totalChineseWords,
        long totalSpanishWords) {
}
