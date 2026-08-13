package com.languageui.api.learning;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LearningService {

    private final Map<UUID, Level> levels = new ConcurrentHashMap<>();
    private final Map<UUID, Lesson> lessons = new ConcurrentHashMap<>();
    private final Map<UUID, VocabularyItem> vocabulary = new ConcurrentHashMap<>();

    public LearningService() {
        for (int sequence = 1; sequence <= 6; sequence++) {
            String code = "HSK" + sequence;
            UUID id = UUID.nameUUIDFromBytes(("zh:" + code).getBytes(StandardCharsets.UTF_8));
            levels.put(id, new Level(id, "zh", code, "HSK Level " + sequence, sequence));
        }
        String[] cefrLevels = {"A1", "A2", "B1", "B2", "C1", "C2"};
        for (int index = 0; index < cefrLevels.length; index++) {
            String code = cefrLevels[index];
            UUID id = UUID.nameUUIDFromBytes(("es:" + code).getBytes(StandardCharsets.UTF_8));
            levels.put(id, new Level(id, "es", code, "Spanish " + code, index + 1));
        }
    }

    public List<Level> levels(String languageCode) {
        return levels.values().stream().filter(level -> level.languageCode().equals(languageCode.toLowerCase()))
                .sorted(Comparator.comparingInt(Level::sequence)).toList();
    }

    public List<Lesson> lessons(UUID levelId) {
        requireLevel(levelId);
        return lessons.values().stream().filter(lesson -> lesson.levelId().equals(levelId))
                .sorted(Comparator.comparingInt(Lesson::sequence)).toList();
    }

    public Lesson createLesson(UUID levelId, CreateLessonRequest request) {
        requireLevel(levelId);
        Lesson lesson = new Lesson(UUID.randomUUID(), levelId, request.title().trim(),
                request.description() == null ? "" : request.description().trim(), request.sequence());
        lessons.put(lesson.id(), lesson);
        return lesson;
    }

    public List<VocabularyItem> vocabulary(UUID lessonId) {
        requireLesson(lessonId);
        return vocabulary.values().stream().filter(item -> item.lessonId().equals(lessonId)).toList();
    }

    public VocabularyItem createVocabulary(UUID lessonId, CreateVocabularyRequest request) {
        requireLesson(lessonId);
        VocabularyItem item = new VocabularyItem(UUID.randomUUID(), lessonId, request.term().trim(),
                request.translation().trim(), request.pronunciation() == null ? "" : request.pronunciation().trim());
        vocabulary.put(item.id(), item);
        return item;
    }

    public VocabularyItem vocabularyById(UUID id) {
        VocabularyItem item = vocabulary.get(id);
        if (item == null) throw new IllegalArgumentException("Vocabulary item not found: " + id);
        return item;
    }

    public boolean lessonExists(UUID id) {
        return lessons.containsKey(id);
    }

    private void requireLevel(UUID id) {
        if (!levels.containsKey(id)) throw new IllegalArgumentException("Level not found: " + id);
    }

    private void requireLesson(UUID id) {
        if (!lessons.containsKey(id)) throw new IllegalArgumentException("Lesson not found: " + id);
    }
}
