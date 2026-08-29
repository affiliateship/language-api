package com.languageui.api.dailyword;

import java.time.LocalDate;
import java.util.List;
import com.languageui.api.word.WordEntry;

public record DailyWordsResponse(LocalDate date, DailyWordPreferences preferences,
                                 List<WordEntry> words, List<DailyWordProgress> progress,
                                 int requestedCount, int deliveredCount, int remainingNewWords,
                                 boolean poolExhausted, boolean sessionCompleted) {
}
