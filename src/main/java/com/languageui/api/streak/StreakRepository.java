package com.languageui.api.streak;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StreakRepository {
    private final JdbcTemplate jdbcTemplate;

    public StreakRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void record(UUID userId, LocalDate date) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_learning_activity WHERE user_id = ? AND activity_date = ?
                """, Integer.class, userId.toString(), date);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE user_learning_activity
                    SET activity_count = activity_count + 1, updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ? AND activity_date = ?
                    """, userId.toString(), date);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO user_learning_activity (user_id, activity_date) VALUES (?, ?)
                    """, userId.toString(), date);
        }
    }

    public List<LocalDate> dates(UUID userId) {
        return jdbcTemplate.query("""
                SELECT activity_date FROM user_learning_activity
                WHERE user_id = ? ORDER BY activity_date
                """, (rs, row) -> rs.getDate(1).toLocalDate(), userId.toString());
    }
}
