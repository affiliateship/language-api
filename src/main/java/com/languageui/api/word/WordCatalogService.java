package com.languageui.api.word;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import com.languageui.api.reading.ReadingAnnotation;

import org.springframework.stereotype.Service;

@Service
public class WordCatalogService {

    private static final Set<String> HSK_LEVELS = Set.of("HSK1", "HSK2", "HSK3", "HSK4", "HSK5", "HSK6");
    private static final Set<String> CEFR_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final WordCatalogRepository repository;

    public WordCatalogService(WordCatalogRepository repository) {
        this.repository = repository;
    }

    public WordEntry create(String languageValue, CreateWordRequest request) {
        String language = normalizeLanguage(languageValue);
        String level = normalizeLevel(language, request.level());
        if (language.equals("Chinese") && (request.pinyin() == null || request.pinyin().isBlank())) {
            throw new IllegalArgumentException("Pinyin is required for Chinese words");
        }
        WordEntry entry = new WordEntry(
                UUID.randomUUID(),
                language,
                request.word().trim(),
                request.englishTranslation().trim(),
                request.pronunciation().trim(),
                optionalValue(request.pinyin()),
                level,
                normalizeWordTypes(request.wordTypes()),
                request.example().trim(),
                request.exampleTranslation().trim());
        if (repository.exists(entry.language(), entry.word(), entry.pinyin())) {
            throw new IllegalStateException("Word already exists: " + entry.word());
        }
        repository.save(entry);
        return entry;
    }

    public boolean createIfAbsent(String language, CreateWordRequest request) {
        String normalizedLanguage = normalizeLanguage(language);
        if (repository.exists(normalizedLanguage, request.word().trim(), optionalValue(request.pinyin()))) {
            return false;
        }
        create(language, request);
        return true;
    }

    public List<WordEntry> createAll(String language, List<CreateWordRequest> requests) {
        // Validate every entry before storing any of the batch.
        requests.forEach(request -> validate(language, request));
        return requests.stream().map(request -> create(language, request)).toList();
    }

    public List<WordEntry> findAll(String languageValue, String levelValue, String wordTypeValue,
            String queryValue, int offset, int limit) {
        String language = normalizeLanguage(languageValue);
        String level = levelValue == null ? null : normalizeLevel(language, levelValue);
        String wordType = normalizedFilter(wordTypeValue);
        String query = normalizedFilter(queryValue);
        if (offset < 0) throw new IllegalArgumentException("Offset must be zero or greater");
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Limit must be between 1 and 500");
        return repository.findByLanguage(language).stream()
                .filter(word -> word.language().equals(language))
                .filter(word -> level == null || word.level().equals(level))
                .filter(word -> wordType == null || word.wordTypes().stream()
                        .anyMatch(type -> type.toLowerCase(Locale.ROOT).equals(wordType)))
                .filter(word -> query == null
                        || word.word().toLowerCase(Locale.ROOT).contains(query)
                        || word.pinyin().toLowerCase(Locale.ROOT).contains(query)
                        || word.pronunciation().toLowerCase(Locale.ROOT).contains(query)
                        || word.englishTranslation().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(WordEntry::level).thenComparing(WordEntry::word))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public long count(String language) {
        return repository.countByLanguage(normalizeLanguage(language));
    }

    public WordEntry findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catalog word not found: " + id));
    }

    public List<WordEntry> findAll(String languageValue, String levelValue) {
        return findAll(languageValue, levelValue, null, null, 0, 500);
    }

    public List<WordEntry> definitions(String languageValue, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Word is required");
        }
        return repository.findExact(normalizeLanguage(languageValue), text.trim());
    }

    public List<ReadingAnnotation> annotate(String languageValue, String text) {
        String language = normalizeLanguage(languageValue);
        List<WordEntry> catalog = repository.findByLanguage(language).stream()
                .sorted(Comparator.comparingInt((WordEntry word) -> word.word().length()).reversed())
                .toList();
        List<ReadingAnnotation> annotations = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            final int position = cursor;
            WordEntry match = catalog.stream()
                    .filter(word -> text.regionMatches(true, position, word.word(), 0, word.word().length()))
                    .findFirst().orElse(null);
            if (match == null) {
                cursor++;
            } else {
                annotations.add(new ReadingAnnotation(cursor, cursor + match.word().length(), match));
                cursor += match.word().length();
            }
        }
        return annotations;
    }

    private void validate(String language, CreateWordRequest request) {
        String normalizedLanguage = normalizeLanguage(language);
        normalizeLevel(normalizedLanguage, request.level());
        if (normalizedLanguage.equals("Chinese") && (request.pinyin() == null || request.pinyin().isBlank())) {
            throw new IllegalArgumentException("Pinyin is required for Chinese words");
        }
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "chinese", "zh" -> "Chinese";
            case "spanish", "es" -> "Spanish";
            default -> throw new IllegalArgumentException("Only Chinese and Spanish words are supported");
        };
    }

    private String normalizeLevel(String language, String levelValue) {
        String level = levelValue.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        Set<String> supportedLevels = language.equals("Chinese") ? HSK_LEVELS : CEFR_LEVELS;
        if (!supportedLevels.contains(level)) {
            throw new IllegalArgumentException(language.equals("Chinese")
                    ? "Chinese level must be HSK1 through HSK6"
                    : "Spanish level must be A1, A2, B1, B2, C1, or C2");
        }
        return level;
    }

    private String optionalValue(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> normalizeWordTypes(List<String> wordTypes) {
        if (wordTypes == null) return List.of();
        return wordTypes.stream()
                .map(String::trim)
                .map(type -> type.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String normalizedFilter(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
