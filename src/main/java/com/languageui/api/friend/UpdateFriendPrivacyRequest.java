package com.languageui.api.friend;

public record UpdateFriendPrivacyRequest(boolean shareStreak, boolean shareCompletedLessons,
                                         boolean shareTopics, boolean shareVocabularyCount,
                                         boolean shareRecentActivity) {
    FriendPrivacySettings toSettings() {
        return new FriendPrivacySettings(shareStreak, shareCompletedLessons, shareTopics,
                shareVocabularyCount, shareRecentActivity);
    }
}
