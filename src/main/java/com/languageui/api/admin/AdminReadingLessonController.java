package com.languageui.api.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.languageui.api.reading.CreateReadingLessonRequest;
import com.languageui.api.reading.ReadingLesson;
import com.languageui.api.reading.ReadingLessonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reading-lessons")
public class AdminReadingLessonController {
    private final ReadingLessonService lessonService;
    private final AdminAuthorizationService adminAuthorization;

    public AdminReadingLessonController(ReadingLessonService lessonService,
            AdminAuthorizationService adminAuthorization) {
        this.lessonService = lessonService;
        this.adminAuthorization = adminAuthorization;
    }

    @GetMapping
    List<ReadingLesson> findAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String language) {
        adminAuthorization.requireAdmin(authorization);
        return lessonService.findAll(language);
    }

    @GetMapping("/{lessonId}")
    ReadingLesson findById(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID lessonId) {
        adminAuthorization.requireAdmin(authorization);
        return lessonService.findById(lessonId);
    }

    @PostMapping
    ResponseEntity<ReadingLesson> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateReadingLessonRequest request) {
        adminAuthorization.requireAdmin(authorization);
        ReadingLesson lesson = lessonService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/reading-lessons/" + lesson.id()))
                .body(lesson);
    }

    @PostMapping("/bulk")
    ResponseEntity<List<ReadingLesson>> createAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody BulkReadingLessonUploadRequest request) {
        adminAuthorization.requireAdmin(authorization);
        return ResponseEntity.status(201).body(lessonService.createAll(request.lessons()));
    }

    @PutMapping("/{lessonId}")
    ReadingLesson update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateReadingLessonRequest request) {
        adminAuthorization.requireAdmin(authorization);
        return lessonService.update(lessonId, request);
    }

    @DeleteMapping("/{lessonId}")
    ResponseEntity<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID lessonId) {
        adminAuthorization.requireAdmin(authorization);
        lessonService.delete(lessonId);
        return ResponseEntity.noContent().build();
    }
}
