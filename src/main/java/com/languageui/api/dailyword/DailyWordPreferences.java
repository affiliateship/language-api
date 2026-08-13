package com.languageui.api.dailyword;

public record DailyWordPreferences(String language, int numberOfWords, boolean doNotRepeat,
                                   String level) {
}
