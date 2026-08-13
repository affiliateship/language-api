package com.languageui.api.friend;

import java.util.List;
import java.util.UUID;

import com.languageui.api.progress.LessonProgress;
import com.languageui.api.progress.LessonProgressService;
import com.languageui.api.progress.LessonProgressStatus;
import com.languageui.api.streak.StreakService;
import com.languageui.api.topic.TopicService;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.AuthorizationException;
import com.languageui.api.user.UserService;
import com.languageui.api.vocabulary.PersonalVocabularyService;
import org.springframework.stereotype.Service;

@Service
public class FriendProfileService {
    private final FriendRepository friendRepository;
    private final FriendPrivacyRepository privacyRepository;
    private final UserService userService;
    private final StreakService streakService;
    private final LessonProgressService progressService;
    private final TopicService topicService;
    private final PersonalVocabularyService vocabularyService;

    public FriendProfileService(FriendRepository friendRepository,
            FriendPrivacyRepository privacyRepository, UserService userService,
            StreakService streakService, LessonProgressService progressService,
            TopicService topicService, PersonalVocabularyService vocabularyService) {
        this.friendRepository = friendRepository;
        this.privacyRepository = privacyRepository;
        this.userService = userService;
        this.streakService = streakService;
        this.progressService = progressService;
        this.topicService = topicService;
        this.vocabularyService = vocabularyService;
    }

    public FriendPrivacySettings privacy(UUID userId) {
        userService.findById(userId);
        return privacyRepository.find(userId).orElse(FriendPrivacySettings.defaults());
    }

    public FriendPrivacySettings updatePrivacy(UUID userId, UpdateFriendPrivacyRequest request) {
        userService.findById(userId);
        FriendPrivacySettings settings = request.toSettings();
        privacyRepository.save(userId, settings);
        return settings;
    }

    public FriendProfile profile(UUID viewerId, UUID friendId) {
        userService.findById(viewerId);
        UserAccount friend = userService.findById(friendId);
        if (!friendRepository.areFriends(viewerId, friendId)) {
            throw new AuthorizationException("Only accepted friends can view this profile");
        }
        FriendPrivacySettings privacy = privacy(friendId);
        List<LessonProgress> completed = privacy.shareCompletedLessons() || privacy.shareRecentActivity()
                ? progressService.findAll(friendId, LessonProgressStatus.COMPLETED) : List.of();
        List<LessonProgress> recent = null;
        if (privacy.shareRecentActivity()) {
            recent = completed.stream().limit(5).toList();
        }
        return new FriendProfile(friend.id(), friend.firstName(), friend.lastName(), friend.displayName(),
                userService.learningLanguages(friendId),
                privacy.shareStreak() ? streakService.get(friendId) : null,
                privacy.shareCompletedLessons() ? completed.size() : null,
                privacy.shareTopics() ? topicService.selected(friendId) : null,
                privacy.shareVocabularyCount() ? vocabularyService.vocabularyIds(friendId).size() : null,
                recent);
    }
}
