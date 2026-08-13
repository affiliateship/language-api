package com.languageui.api.studygroup;
import java.util.List;
import java.util.UUID;
public record StudyGroupProgress(UUID groupId, UUID lessonId, int completedMembers,
                                 int totalMembers, List<StudyGroupMemberProgress> members) { }
