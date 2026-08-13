package com.languageui.api.streak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.languageui.api.user.StreakResponse;
import org.junit.jupiter.api.Test;

class StreakServiceTest {

    @Test
    void calculatesCurrentAndLongestConsecutiveDayRuns() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6));

        StreakResponse result = StreakService.calculate(dates, LocalDate.of(2026, 8, 7));

        assertThat(result.currentDays()).isEqualTo(3);
        assertThat(result.longestDays()).isEqualTo(3);
        assertThat(result.lastActivityDate()).isEqualTo(LocalDate.of(2026, 8, 6));
    }

    @Test
    void resetsCurrentStreakAfterMissingAFullDay() {
        StreakResponse result = StreakService.calculate(
                List.of(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6)),
                LocalDate.of(2026, 8, 8));

        assertThat(result.currentDays()).isZero();
        assertThat(result.longestDays()).isEqualTo(2);
    }
}
