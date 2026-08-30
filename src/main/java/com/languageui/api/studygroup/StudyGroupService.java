package com.languageui.api.studygroup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.languageui.api.friend.FriendRepository;
import com.languageui.api.progress.LessonProgressService;
import com.languageui.api.progress.LessonProgressStatus;
import com.languageui.api.reading.ReadingLessonService;
import com.languageui.api.user.AuthorizationException;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StudyGroupService {

    private final StudyGroupRepository repository;
    private final FriendRepository friendRepository;
    private final UserService userService;
    private final ReadingLessonService readingLessonService;
    private final LessonProgressService progressService;

    public StudyGroupService(StudyGroupRepository repository, FriendRepository friendRepository,
            UserService userService, ReadingLessonService readingLessonService,
            LessonProgressService progressService) {
        this.repository = repository;
        this.friendRepository = friendRepository;
        this.userService = userService;
        this.readingLessonService = readingLessonService;
        this.progressService = progressService;
    }

    @Transactional
    public StudyGroup create(UUID ownerId, CreateStudyGroupRequest request) {
        userService.findById(ownerId);
        StudyGroup group = new StudyGroup(UUID.randomUUID(), ownerId, request.name().trim(),
                normalizeLanguage(request.language()), normalizeLevel(request.level()), null,
                StudyGroupStatus.ACTIVE, LocalDateTime.now());
        repository.create(group);
        if (request.friendIds() != null && !request.friendIds().isEmpty()) {
            invite(ownerId, group.id(), request.friendIds());
        }
        log.info("study_group_created groupId={} ownerId={} invitedCount={}", group.id(), ownerId,
                request.friendIds() == null ? 0 : request.friendIds().size());
        return requireGroup(group.id());
    }

    public List<StudyGroup> groups(UUID userId) {
        userService.findById(userId);
        return repository.groupsFor(userId);
    }

    public StudyGroup get(UUID userId, UUID groupId) {
        StudyGroup group = requireGroup(groupId);
        requireMember(group, userId);
        return group;
    }

    public List<StudyGroupInvitation> invite(UUID ownerId, UUID groupId, Set<UUID> friendIds) {
        StudyGroup group = requireOwner(ownerId, groupId);
        UserAccount inviter = userService.findById(ownerId);
        return friendIds.stream()
                .map(friendId -> createInvitation(group, inviter, friendId))
                .toList();
    }

    public List<StudyGroupInvitation> invitations(UUID userId) {
        userService.findById(userId);
        return repository.invitations(userId).stream().map(this::toInvitation).toList();
    }

    @Transactional
    public StudyGroup accept(UUID userId, UUID invitationId) {
        StudyGroupRepository.InvitationRow invitation = requirePendingInvitation(userId, invitationId);
        repository.addMember(invitation.groupId(), userId);
        repository.invitationStatus(invitationId, "ACCEPTED");
        log.info("study_group_invitation_accepted invitationId={} groupId={} userId={}",
                invitationId, invitation.groupId(), userId);
        return requireGroup(invitation.groupId());
    }

    public void decline(UUID userId, UUID invitationId) {
        StudyGroupRepository.InvitationRow invitation = requirePendingInvitation(userId, invitationId);
        repository.invitationStatus(invitation.id(), "DECLINED");
        log.info("study_group_invitation_declined invitationId={} userId={}", invitationId, userId);
    }

    public StudyGroup assign(UUID ownerId, UUID groupId, UUID lessonId) {
        requireOwner(ownerId, groupId);
        readingLessonService.findById(lessonId);
        repository.assignLesson(groupId, lessonId);
        log.info("study_group_lesson_assigned groupId={} lessonId={} ownerId={}",
                groupId, lessonId, ownerId);
        return requireGroup(groupId);
    }

    public StudyGroupProgress groupProgress(UUID userId, UUID groupId) {
        StudyGroup group = get(userId, groupId);
        List<UUID> memberIds = repository.memberIds(groupId);
        if (group.lessonId() == null) {
            return new StudyGroupProgress(groupId, null, 0, memberIds.size(), List.of());
        }
        List<StudyGroupMemberProgress> members = memberIds.stream()
                .map(memberId -> memberProgress(memberId, group.lessonId()))
                .toList();
        int completed = (int) members.stream()
                .filter(member -> member.lessonStatus() == LessonProgressStatus.COMPLETED)
                .count();
        return new StudyGroupProgress(groupId, group.lessonId(), completed, members.size(), members);
    }

    private StudyGroupInvitation createInvitation(StudyGroup group, UserAccount inviter, UUID friendId) {
        userService.findById(friendId);
        if (!friendRepository.areFriends(inviter.id(), friendId)) {
            throw new IllegalArgumentException("Only accepted friends can be invited: " + friendId);
        }
        if (repository.isMember(group.id(), friendId)) {
            throw new IllegalStateException("User is already a member: " + friendId);
        }
        StudyGroupInvitation invitation = repository.invite(group.id(), inviter.id(), friendId,
                group.name(), inviter.username());
        log.info("study_group_invitation_created invitationId={} groupId={} inviteeId={}",
                invitation.id(), group.id(), friendId);
        return invitation;
    }

    private StudyGroupInvitation toInvitation(StudyGroupRepository.InvitationRow row) {
        StudyGroup group = requireGroup(row.groupId());
        UserAccount inviter = userService.findById(row.inviterId());
        return new StudyGroupInvitation(row.id(), group.id(), group.name(), inviter.id(),
                inviter.username(), row.status(), group.createdAt());
    }

    private StudyGroupMemberProgress memberProgress(UUID userId, UUID lessonId) {
        UserAccount user = userService.findById(userId);
        LessonProgressStatus status = repositoryStatus(userId, lessonId);
        return new StudyGroupMemberProgress(userId, user.username(), status);
    }

    private LessonProgressStatus repositoryStatus(UUID userId, UUID lessonId) {
        try {
            return progressService.find(userId, lessonId).status();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private StudyGroupRepository.InvitationRow requirePendingInvitation(UUID userId, UUID invitationId) {
        StudyGroupRepository.InvitationRow invitation = repository.invitation(invitationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invitation not found: " + invitationId));
        if (!invitation.inviteeId().equals(userId)) {
            throw new AuthorizationException("Invitation does not belong to you");
        }
        if (!invitation.status().equals("PENDING")) {
            throw new IllegalStateException("Invitation is no longer pending");
        }
        return invitation;
    }

    private StudyGroup requireGroup(UUID groupId) {
        return repository.find(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Study group not found: " + groupId));
    }

    private StudyGroup requireOwner(UUID userId, UUID groupId) {
        StudyGroup group = requireGroup(groupId);
        if (!group.ownerId().equals(userId)) {
            throw new AuthorizationException("Only the group owner can do this");
        }
        return group;
    }

    private void requireMember(StudyGroup group, UUID userId) {
        if (!repository.isMember(group.id(), userId)) {
            throw new AuthorizationException("Only group members can view this group");
        }
    }

    private String normalizeLanguage(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "zh", "chinese" -> "Chinese";
            case "es", "spanish" -> "Spanish";
            default -> throw new IllegalArgumentException("Only Chinese and Spanish are supported");
        };
    }

    private String normalizeLevel(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
