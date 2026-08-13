package com.languageui.api.friend;

public record FriendPrivacySettings(boolean shareStreak, boolean shareCompletedLessons,
                                    boolean shareTopics, boolean shareVocabularyCount,
                                    boolean shareRecentActivity) {
    public static FriendPrivacySettings defaults() {
        return new FriendPrivacySettings(true, true, true, false, false);
    }
}
