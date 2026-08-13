package com.languageui.api.friend;

import java.util.List;
import java.util.UUID;

import com.languageui.api.language.Language;
import com.languageui.api.progress.LessonProgress;
import com.languageui.api.topic.Topic;
import com.languageui.api.user.StreakResponse;

public record FriendProfile(UUID id, String firstName, String lastName, String displayName,
                            List<Language> learningLanguages, StreakResponse streak,
                            Integer completedLessons, List<Topic> selectedTopics,
                            Integer vocabularyCount, List<LessonProgress> recentActivity) {
}
