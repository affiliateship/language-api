package com.languageui.api.dailyword;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateDailyWordPreferencesRequest(
        @NotBlank String language,
        @Min(1) @Max(20) int numberOfWords,
        boolean doNotRepeat,
        String level) {
}
