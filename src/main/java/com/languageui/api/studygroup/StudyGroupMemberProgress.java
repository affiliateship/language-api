package com.languageui.api.studygroup;
import java.util.UUID;
import com.languageui.api.progress.LessonProgressStatus;
public record StudyGroupMemberProgress(UUID userId, String username,
                                       LessonProgressStatus lessonStatus) { }
