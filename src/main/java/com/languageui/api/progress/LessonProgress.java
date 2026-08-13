package com.languageui.api.progress;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonProgress(UUID lessonId, LessonProgressStatus status,
                             LocalDateTime startedAt, LocalDateTime completedAt) {
}
