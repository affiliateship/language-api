package com.languageui.api.reading;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.languageui.api.word.WordCatalogService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingLessonService {
    private final ReadingLessonRepository repository;
    private final WordCatalogService wordCatalogService;

    public ReadingLessonService(ReadingLessonRepository repository,
            WordCatalogService wordCatalogService) {
        this.repository = repository;
        this.wordCatalogService = wordCatalogService;
    }

    @Transactional
    public ReadingLesson create(CreateReadingLessonRequest request) {
        ReadingLesson lesson = lesson(UUID.randomUUID(), request);
        repository.save(lesson);
        return lesson;
    }

    @Transactional
    public List<ReadingLesson> createAll(List<CreateReadingLessonRequest> requests) {
        return requests.stream().map(this::create).toList();
    }

    @Transactional
    public ReadingLesson update(UUID id, CreateReadingLessonRequest request) {
        findById(id);
        ReadingLesson lesson = lesson(id, request);
        repository.update(lesson);
        return lesson;
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("Reading lesson not found: " + id);
        }
    }

    private ReadingLesson lesson(UUID id, CreateReadingLessonRequest request) {
        String originalText = request.originalText().trim();
        return new ReadingLesson(id, normalizeLanguage(request.language()),
                request.level().trim().toUpperCase(Locale.ROOT), request.lessonType(),
                request.title().trim(), originalText, request.englishTranslation().trim(),
                associate(originalText, request.keyWords()));
    }

    public List<ReadingLesson> findAll(String language) {
        return repository.findAll(language == null || language.isBlank() ? null : normalizeLanguage(language));
    }

    public ReadingLesson findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading lesson not found: " + id));
    }

    public boolean exists(UUID id) {
        return repository.findById(id).isPresent();
    }

    public List<ReadingAnnotation> annotations(UUID lessonId) {
        ReadingLesson lesson = findById(lessonId);
        return wordCatalogService.annotate(lesson.language(), lesson.originalText());
    }

    private List<ReadingWord> associate(String original, List<CreateReadingWordRequest> words) {
        int[] cursor = {0};
        return words.stream().map(word -> {
            String text = word.original().trim();
            int start = original.indexOf(text, cursor[0]);
            if (start < 0) {
                throw new IllegalArgumentException(
                        "Word is missing or out of order in originalText: " + text);
            }
            int end = start + text.length();
            cursor[0] = end;
            return new ReadingWord(text, word.englishTranslation().trim(),
                    word.pronunciation().trim(), start, end);
        }).toList();
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "chinese", "zh" -> "Chinese";
            case "spanish", "es" -> "Spanish";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
