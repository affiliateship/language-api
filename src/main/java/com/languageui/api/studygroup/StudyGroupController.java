package com.languageui.api.studygroup;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.languageui.api.user.AuthService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class StudyGroupController {
    private final StudyGroupService service;
    private final AuthService authService;

    public StudyGroupController(StudyGroupService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping("/study-groups")
    ResponseEntity<StudyGroup> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateStudyGroupRequest request) {
        StudyGroup group = service.create(authService.currentUserId(authorization), request);
        return ResponseEntity.created(URI.create("/api/me/study-groups/" + group.id())).body(group);
    }

    @GetMapping("/study-groups")
    List<StudyGroup> groups(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.groups(authService.currentUserId(authorization));
    }

    @GetMapping("/study-groups/{groupId}")
    StudyGroup group(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID groupId) {
        return service.get(authService.currentUserId(authorization), groupId);
    }

    @PostMapping("/study-groups/{groupId}/invitations")
    List<StudyGroupInvitation> invite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteFriendsRequest request) {
        return service.invite(authService.currentUserId(authorization), groupId, request.friendIds());
    }

    @GetMapping("/study-group-invitations")
    List<StudyGroupInvitation> invitations(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.invitations(authService.currentUserId(authorization));
    }

    @PostMapping("/study-group-invitations/{invitationId}/accept")
    StudyGroup accept(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID invitationId) {
        return service.accept(authService.currentUserId(authorization), invitationId);
    }

    @DeleteMapping("/study-group-invitations/{invitationId}")
    ResponseEntity<Void> decline(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID invitationId) {
        service.decline(authService.currentUserId(authorization), invitationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/study-groups/{groupId}/lesson")
    StudyGroup assignLesson(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID groupId,
            @Valid @RequestBody AssignLessonRequest request) {
        return service.assign(authService.currentUserId(authorization), groupId, request.lessonId());
    }

    @GetMapping("/study-groups/{groupId}/progress")
    StudyGroupProgress progress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID groupId) {
        return service.groupProgress(authService.currentUserId(authorization), groupId);
    }
}
