package com.languageui.api.dailyword;

import java.util.UUID;

public record DailyWordProgress(
        UUID wordId,
        DailyWordStatus status,
        int answerCount,
        int correctAnswerCount) {
}

