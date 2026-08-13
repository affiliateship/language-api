package com.languageui.api.progress;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LessonProgressRepository {
    private final JdbcTemplate jdbcTemplate;

    public LessonProgressRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Optional<LessonProgress> find(UUID userId, UUID lessonId) {
        return jdbcTemplate.query("""
                SELECT * FROM lesson_progress WHERE user_id = ? AND lesson_id = ?
                """, this::map, userId.toString(), lessonId.toString()).stream().findFirst();
    }

    public List<LessonProgress> findAll(UUID userId, LessonProgressStatus status) {
        String sql = "SELECT * FROM lesson_progress WHERE user_id = ?" +
                (status == null ? "" : " AND status = ?") + " ORDER BY updated_at DESC";
        return status == null ? jdbcTemplate.query(sql, this::map, userId.toString())
                : jdbcTemplate.query(sql, this::map, userId.toString(), status.name());
    }

    public void insert(UUID userId, UUID lessonId, LessonProgressStatus status, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO lesson_progress
                    (user_id, lesson_id, status, started_at, completed_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId.toString(), lessonId.toString(), status.name(), now,
                status == LessonProgressStatus.COMPLETED ? now : null, now);
    }

    public void complete(UUID userId, UUID lessonId, LocalDateTime now) {
        jdbcTemplate.update("""
                UPDATE lesson_progress SET status = 'COMPLETED', completed_at = ?, updated_at = ?
                WHERE user_id = ? AND lesson_id = ?
                """, now, now, userId.toString(), lessonId.toString());
    }

    public void deleteAll() { jdbcTemplate.update("DELETE FROM lesson_progress"); }

    private LessonProgress map(ResultSet rs, int row) throws SQLException {
        var completed = rs.getTimestamp("completed_at");
        return new LessonProgress(UUID.fromString(rs.getString("lesson_id")),
                LessonProgressStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at").toLocalDateTime(),
                completed == null ? null : completed.toLocalDateTime());
    }
}
