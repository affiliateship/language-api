package com.languageui.api.friend;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FriendPrivacyRepository {
    private final JdbcTemplate jdbcTemplate;

    public FriendPrivacyRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Optional<FriendPrivacySettings> find(UUID userId) {
        return jdbcTemplate.query("SELECT * FROM friend_privacy_settings WHERE user_id = ?",
                (rs, row) -> new FriendPrivacySettings(rs.getBoolean("share_streak"),
                        rs.getBoolean("share_completed_lessons"), rs.getBoolean("share_topics"),
                        rs.getBoolean("share_vocabulary_count"), rs.getBoolean("share_recent_activity")),
                userId.toString()).stream().findFirst();
    }

    public void save(UUID userId, FriendPrivacySettings settings) {
        if (find(userId).isPresent()) {
            jdbcTemplate.update("""
                    UPDATE friend_privacy_settings SET share_streak = ?, share_completed_lessons = ?,
                        share_topics = ?, share_vocabulary_count = ?, share_recent_activity = ?,
                        updated_at = CURRENT_TIMESTAMP WHERE user_id = ?
                    """, settings.shareStreak(), settings.shareCompletedLessons(), settings.shareTopics(),
                    settings.shareVocabularyCount(), settings.shareRecentActivity(), userId.toString());
        } else {
            jdbcTemplate.update("""
                    INSERT INTO friend_privacy_settings
                        (user_id, share_streak, share_completed_lessons, share_topics,
                         share_vocabulary_count, share_recent_activity)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, userId.toString(), settings.shareStreak(), settings.shareCompletedLessons(),
                    settings.shareTopics(), settings.shareVocabularyCount(), settings.shareRecentActivity());
        }
    }
}
