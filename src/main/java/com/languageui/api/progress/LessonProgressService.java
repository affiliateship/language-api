package com.languageui.api.progress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.languageui.api.learning.LearningService;
import com.languageui.api.reading.ReadingLessonService;
import com.languageui.api.streak.StreakService;
import com.languageui.api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonProgressService {
    private final LessonProgressRepository repository;
    private final UserService userService;
    private final ReadingLessonService readingLessonService;
    private final LearningService learningService;
    private final StreakService streakService;

    public LessonProgressService(LessonProgressRepository repository, UserService userService,
            ReadingLessonService readingLessonService, LearningService learningService,
            StreakService streakService) {
        this.repository = repository;
        this.userService = userService;
        this.readingLessonService = readingLessonService;
        this.learningService = learningService;
        this.streakService = streakService;
    }

    @Transactional
    public LessonProgress update(UUID userId, UUID lessonId, LessonProgressStatus status) {
        userService.findById(userId);
        requireLesson(lessonId);
        LessonProgress existing = repository.find(userId, lessonId).orElse(null);
        if (existing == null) {
            repository.insert(userId, lessonId, status, LocalDateTime.now());
        } else if (existing.status() == LessonProgressStatus.COMPLETED
                && status == LessonProgressStatus.STARTED) {
            throw new IllegalStateException("A completed lesson cannot be moved back to started");
        } else if (existing.status() != status) {
            repository.complete(userId, lessonId, LocalDateTime.now());
        }
        streakService.record(userId);
        return repository.find(userId, lessonId).orElseThrow();
    }

    public LessonProgress find(UUID userId, UUID lessonId) {
        userService.findById(userId);
        return repository.find(userId, lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson progress not found: " + lessonId));
    }

    public List<LessonProgress> findAll(UUID userId, LessonProgressStatus status) {
        userService.findById(userId);
        return repository.findAll(userId, status);
    }

    private void requireLesson(UUID lessonId) {
        if (!readingLessonService.exists(lessonId) && !learningService.lessonExists(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }
    }
}
