package com.languageui.api.studygroup;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
public record AssignLessonRequest(@NotNull UUID lessonId) { }
