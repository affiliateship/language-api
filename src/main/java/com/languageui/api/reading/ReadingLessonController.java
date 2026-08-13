package com.languageui.api.reading;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reading-lessons")
public class ReadingLessonController {
    private final ReadingLessonService service;

    public ReadingLessonController(ReadingLessonService service) { this.service = service; }

    @GetMapping
    List<ReadingLesson> findAll(@RequestParam(required = false) String language) {
        return service.findAll(language);
    }

    @GetMapping("/{lessonId}")
    ReadingLesson findById(@PathVariable UUID lessonId) { return service.findById(lessonId); }

    @GetMapping("/{lessonId}/annotations")
    List<ReadingAnnotation> annotations(@PathVariable UUID lessonId) {
        return service.annotations(lessonId);
    }

    @PostMapping
    ResponseEntity<ReadingLesson> create(@Valid @RequestBody CreateReadingLessonRequest request) {
        ReadingLesson lesson = service.create(request);
        return ResponseEntity.created(java.net.URI.create("/api/v1/reading-lessons/" + lesson.id()))
                .body(lesson);
    }
}
