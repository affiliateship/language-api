package com.languageui.api.feedback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackRepository {
    private final JdbcTemplate jdbcTemplate;

    public FeedbackRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Feedback save(UUID userId, CreateFeedbackRequest request) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO feedback (id, user_id, category, title, message, status)
                VALUES (?, ?, ?, ?, ?, 'SUBMITTED')
                """, id.toString(), userId.toString(), request.category().name(),
                request.title().trim(), request.message().trim());
        return findByUser(userId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public List<Feedback> findByUser(UUID userId) {
        return jdbcTemplate.query("SELECT * FROM feedback WHERE user_id = ? ORDER BY created_at DESC",
                this::map, userId.toString());
    }

    private Feedback map(ResultSet rs, int row) throws SQLException {
        return new Feedback(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("user_id")),
                FeedbackCategory.valueOf(rs.getString("category")), rs.getString("title"),
                rs.getString("message"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
