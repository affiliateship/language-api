package com.languageui.api.reading;

import com.languageui.api.word.WordEntry;

public record ReadingAnnotation(int startIndex, int endIndex, WordEntry definition) {
}
