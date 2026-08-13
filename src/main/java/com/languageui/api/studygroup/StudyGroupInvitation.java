package com.languageui.api.studygroup;
import java.time.LocalDateTime;
import java.util.UUID;
public record StudyGroupInvitation(UUID id, UUID groupId, String groupName, UUID inviterId,
                                   String inviterDisplayName, String status, LocalDateTime createdAt) { }
