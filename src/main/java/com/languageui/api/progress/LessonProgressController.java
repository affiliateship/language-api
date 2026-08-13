package com.languageui.api.progress;

import java.util.List;
import java.util.UUID;

import com.languageui.api.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/lesson-progress")
public class LessonProgressController {
    private final LessonProgressService service;
    private final AuthService authService;

    public LessonProgressController(LessonProgressService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    List<LessonProgress> findAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) LessonProgressStatus status) {
        return service.findAll(authService.currentUserId(authorization), status);
    }

    @GetMapping("/{lessonId}")
    LessonProgress find(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID lessonId) {
        return service.find(authService.currentUserId(authorization), lessonId);
    }

    @PutMapping("/{lessonId}")
    LessonProgress update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonProgressRequest request) {
        return service.update(authService.currentUserId(authorization), lessonId, request.status());
    }
}
