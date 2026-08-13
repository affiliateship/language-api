package com.languageui.api.progress;

import jakarta.validation.constraints.NotNull;

public record UpdateLessonProgressRequest(@NotNull LessonProgressStatus status) {
}
