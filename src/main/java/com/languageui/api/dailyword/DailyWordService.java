package com.languageui.api.dailyword;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.languageui.api.user.UserService;
import com.languageui.api.word.WordEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyWordService {
    private static final Set<String> HSK_LEVELS = Set.of("HSK1", "HSK2", "HSK3", "HSK4", "HSK5", "HSK6");
    private static final Set<String> CEFR_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final DailyWordPreferences DEFAULTS =
            new DailyWordPreferences("Chinese", 1, true, null);

    private final DailyWordRepository repository;
    private final UserService userService;

    public DailyWordService(DailyWordRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public DailyWordPreferences preferences(UUID userId) {
        userService.findById(userId);
        return repository.preferences(userId).orElse(DEFAULTS);
    }

    @Transactional
    public DailyWordPreferences update(UUID userId, UpdateDailyWordPreferencesRequest request) {
        userService.findById(userId);
        String language = normalizeLanguage(request.language());
        String level = normalizeLevel(language, request.level());
        DailyWordPreferences preferences = new DailyWordPreferences(
                language, request.numberOfWords(), request.doNotRepeat(), level);
        repository.savePreferences(userId, preferences);
        repository.deleteDeliveries(userId, LocalDate.now());
        return preferences;
    }

    @Transactional
    public DailyWordsResponse dailyWords(UUID userId) {
        LocalDate today = LocalDate.now();
        DailyWordPreferences preferences = preferences(userId);
        List<WordEntry> words = repository.delivered(userId, today);
        if (words.isEmpty()) {
            words = repository.candidates(userId, preferences, preferences.numberOfWords());
            repository.saveDeliveries(userId, today, words);
        }
        return new DailyWordsResponse(today, preferences, words);
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "chinese", "zh" -> "Chinese";
            case "spanish", "es" -> "Spanish";
            default -> throw new IllegalArgumentException("Only Chinese and Spanish are supported");
        };
    }

    private String normalizeLevel(String language, String value) {
        if (value == null || value.isBlank()) return null;
        String level = value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        Set<String> supported = language.equals("Chinese") ? HSK_LEVELS : CEFR_LEVELS;
        if (!supported.contains(level)) {
            throw new IllegalArgumentException("Level is not valid for " + language + ": " + value);
        }
        return level;
    }
}
