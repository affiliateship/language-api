package com.languageui.api.reading;

public record ReadingWord(String original, String englishTranslation, String pronunciation,
                          int startIndex, int endIndex) {
}
