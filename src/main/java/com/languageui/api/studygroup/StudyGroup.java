package com.languageui.api.studygroup;
import java.time.LocalDateTime;
import java.util.UUID;
public record StudyGroup(UUID id, UUID ownerId, String name, String language, String level,
                         UUID lessonId, StudyGroupStatus status, LocalDateTime createdAt) { }
