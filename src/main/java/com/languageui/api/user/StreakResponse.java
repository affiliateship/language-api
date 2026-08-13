package com.languageui.api.user;

import java.time.LocalDate;

public record StreakResponse(int currentDays, int longestDays, LocalDate lastActivityDate) {
}
