package com.languageui.api.streak;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.languageui.api.user.StreakResponse;
import org.springframework.stereotype.Service;

@Service
public class StreakService {
    private final StreakRepository repository;

    public StreakService(StreakRepository repository) {
        this.repository = repository;
    }

    public StreakResponse record(UUID userId) {
        repository.record(userId, LocalDate.now());
        return get(userId);
    }

    public StreakResponse get(UUID userId) {
        return calculate(repository.dates(userId), LocalDate.now());
    }

    static StreakResponse calculate(List<LocalDate> dates, LocalDate today) {
        if (dates.isEmpty()) {
            return new StreakResponse(0, 0, null);
        }
        int longest = 1;
        int run = 1;
        for (int index = 1; index < dates.size(); index++) {
            if (ChronoUnit.DAYS.between(dates.get(index - 1), dates.get(index)) == 1) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 1;
            }
        }

        LocalDate last = dates.get(dates.size() - 1);
        int current = 0;
        long daysSinceActivity = ChronoUnit.DAYS.between(last, today);
        if (daysSinceActivity == 0 || daysSinceActivity == 1) {
            current = 1;
            for (int index = dates.size() - 1; index > 0; index--) {
                if (ChronoUnit.DAYS.between(dates.get(index - 1), dates.get(index)) != 1) break;
                current++;
            }
        }
        return new StreakResponse(current, longest, last);
    }
}
