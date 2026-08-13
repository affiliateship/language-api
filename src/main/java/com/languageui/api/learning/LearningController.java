package com.languageui.api.learning;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) { this.learningService = learningService; }

    @GetMapping("/languages/{languageCode}/levels")
    List<Level> levels(@PathVariable String languageCode) { return learningService.levels(languageCode); }

    @GetMapping("/levels/{levelId}/lessons")
    List<Lesson> lessons(@PathVariable UUID levelId) { return learningService.lessons(levelId); }

    @PostMapping("/levels/{levelId}/lessons")
    ResponseEntity<Lesson> createLesson(@PathVariable UUID levelId,
            @Valid @RequestBody CreateLessonRequest request) {
        Lesson lesson = learningService.createLesson(levelId, request);
        return ResponseEntity.created(URI.create("/api/v1/lessons/" + lesson.id())).body(lesson);
    }

    @GetMapping("/lessons/{lessonId}/vocabulary")
    List<VocabularyItem> vocabulary(@PathVariable UUID lessonId) { return learningService.vocabulary(lessonId); }

    @PostMapping("/lessons/{lessonId}/vocabulary")
    ResponseEntity<VocabularyItem> createVocabulary(@PathVariable UUID lessonId,
            @Valid @RequestBody CreateVocabularyRequest request) {
        VocabularyItem item = learningService.createVocabulary(lessonId, request);
        return ResponseEntity.created(URI.create("/api/v1/vocabulary/" + item.id())).body(item);
    }
}
