package com.languageui.api.dailyword;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import com.languageui.api.word.WordEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.languageui.api.word.WordExample;

@Repository
public class DailyWordRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public DailyWordRepository(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    public Optional<DailyWordPreferences> preferences(UUID userId) {
        return jdbcTemplate.query("SELECT * FROM daily_word_preferences WHERE user_id = ?",
                (rs, row) -> new DailyWordPreferences(rs.getString("language"),
                        rs.getInt("number_of_words"), rs.getBoolean("do_not_repeat"),
                        rs.getString("level")), userId.toString()).stream().findFirst();
    }

    public void savePreferences(UUID userId, DailyWordPreferences preferences) {
        if (preferences(userId).isPresent()) {
            jdbcTemplate.update("""
                    UPDATE daily_word_preferences SET language = ?, number_of_words = ?,
                        do_not_repeat = ?, level = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?
                    """, preferences.language(), preferences.numberOfWords(), preferences.doNotRepeat(),
                    preferences.level(), userId.toString());
        } else {
            jdbcTemplate.update("""
                    INSERT INTO daily_word_preferences
                        (user_id, language, number_of_words, do_not_repeat, level)
                    VALUES (?, ?, ?, ?, ?)
                    """, userId.toString(), preferences.language(), preferences.numberOfWords(),
                    preferences.doNotRepeat(), preferences.level());
        }
    }

    public List<WordEntry> delivered(UUID userId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT w.* FROM daily_word_deliveries d
                JOIN word_entries w ON w.id = d.word_id
                WHERE d.user_id = ? AND d.delivery_date = ? ORDER BY d.sequence_number
                """, this::mapWord, userId.toString(), date);
    }

    public List<WordEntry> candidates(UUID userId, DailyWordPreferences preferences, int limit) {
        String levelClause = preferences.level() == null ? "" : " AND w.level = ?";
        String historyClause = preferences.doNotRepeat() ? """
                 AND NOT EXISTS (SELECT 1 FROM daily_word_deliveries history
                    WHERE history.user_id = ? AND history.word_id = w.id)
                """ : "";
        String sql = "SELECT w.* FROM word_entries w WHERE w.language = ?" + levelClause +
                historyClause + " ORDER BY RANDOM() LIMIT ?";
        if (preferences.level() != null && preferences.doNotRepeat()) {
            return jdbcTemplate.query(sql, this::mapWord, preferences.language(), preferences.level(),
                    userId.toString(), limit);
        }
        if (preferences.level() != null) {
            return jdbcTemplate.query(sql, this::mapWord, preferences.language(), preferences.level(), limit);
        }
        if (preferences.doNotRepeat()) {
            return jdbcTemplate.query(sql, this::mapWord, preferences.language(), userId.toString(), limit);
        }
        return jdbcTemplate.query(sql, this::mapWord, preferences.language(), limit);
    }

    public int candidateCount(UUID userId, DailyWordPreferences preferences) {
        String levelClause = preferences.level() == null ? "" : " AND w.level = ?";
        String historyClause = preferences.doNotRepeat() ? """
                 AND NOT EXISTS (SELECT 1 FROM daily_word_deliveries history
                    WHERE history.user_id = ? AND history.word_id = w.id)
                """ : "";
        String sql = "SELECT COUNT(*) FROM word_entries w WHERE w.language = ?" + levelClause + historyClause;
        Integer count;
        if (preferences.level() != null && preferences.doNotRepeat()) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, preferences.language(),
                    preferences.level(), userId.toString());
        } else if (preferences.level() != null) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, preferences.language(), preferences.level());
        } else if (preferences.doNotRepeat()) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, preferences.language(), userId.toString());
        } else {
            count = jdbcTemplate.queryForObject(sql, Integer.class, preferences.language());
        }
        return count == null ? 0 : count;
    }

    public List<DailyWordProgress> progress(UUID userId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT word_id, viewed_at, answer_count, correct_answer_count, completed_at
                FROM daily_word_deliveries
                WHERE user_id = ? AND delivery_date = ? ORDER BY sequence_number
                """, (rs, row) -> {
                    int answers = rs.getInt("answer_count");
                    DailyWordStatus status = rs.getTimestamp("completed_at") != null
                            ? DailyWordStatus.COMPLETED
                            : answers > 0 ? DailyWordStatus.PRACTICING
                            : rs.getTimestamp("viewed_at") != null ? DailyWordStatus.VIEWED
                            : DailyWordStatus.NEW;
                    return new DailyWordProgress(UUID.fromString(rs.getString("word_id")), status,
                            answers, rs.getInt("correct_answer_count"));
                }, userId.toString(), date);
    }

    public void markViewed(UUID userId, LocalDate date, UUID wordId, LocalDateTime now) {
        requireTodayDelivery(jdbcTemplate.update("""
                UPDATE daily_word_deliveries SET viewed_at = COALESCE(viewed_at, ?)
                WHERE user_id = ? AND delivery_date = ? AND word_id = ?
                """, now, userId.toString(), date, wordId.toString()), wordId);
    }

    public void recordAnswer(UUID userId, LocalDate date, UUID wordId, boolean correct,
            LocalDateTime now) {
        requireTodayDelivery(jdbcTemplate.update("""
                UPDATE daily_word_deliveries
                SET viewed_at = COALESCE(viewed_at, ?), answer_count = answer_count + 1,
                    correct_answer_count = correct_answer_count + ?
                WHERE user_id = ? AND delivery_date = ? AND word_id = ?
                """, now, correct ? 1 : 0, userId.toString(), date, wordId.toString()), wordId);
    }

    public boolean complete(UUID userId, LocalDate date, UUID wordId, LocalDateTime now) {
        int updated = jdbcTemplate.update("""
                UPDATE daily_word_deliveries
                SET viewed_at = COALESCE(viewed_at, ?), completed_at = ?
                WHERE user_id = ? AND delivery_date = ? AND word_id = ? AND completed_at IS NULL
                """, now, now, userId.toString(), date, wordId.toString());
        if (updated == 0 && !deliveryExists(userId, date, wordId)) {
            throw new IllegalArgumentException("Word is not in today's daily selection: " + wordId);
        }
        return updated == 1;
    }

    public boolean sessionCompleted(UUID userId, LocalDate date) {
        Integer incomplete = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM daily_word_deliveries
                WHERE user_id = ? AND delivery_date = ? AND completed_at IS NULL
                """, Integer.class, userId.toString(), date);
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM daily_word_deliveries WHERE user_id = ? AND delivery_date = ?
                """, Integer.class, userId.toString(), date);
        return total != null && total > 0 && incomplete != null && incomplete == 0;
    }

    private boolean deliveryExists(UUID userId, LocalDate date, UUID wordId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM daily_word_deliveries
                WHERE user_id = ? AND delivery_date = ? AND word_id = ?
                """, Integer.class, userId.toString(), date, wordId.toString());
        return count != null && count > 0;
    }

    private void requireTodayDelivery(int updated, UUID wordId) {
        if (updated == 0) {
            throw new IllegalArgumentException("Word is not in today's daily selection: " + wordId);
        }
    }

    public void saveDeliveries(UUID userId, LocalDate date, List<WordEntry> words) {
        for (int sequence = 0; sequence < words.size(); sequence++) {
            jdbcTemplate.update("""
                    INSERT INTO daily_word_deliveries
                        (user_id, delivery_date, word_id, sequence_number) VALUES (?, ?, ?, ?)
                    """, userId.toString(), date, words.get(sequence).id().toString(), sequence);
        }
    }

    public void deleteDeliveries(UUID userId, LocalDate date) {
        jdbcTemplate.update("DELETE FROM daily_word_deliveries WHERE user_id = ? AND delivery_date = ?",
                userId.toString(), date);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM daily_word_deliveries");
        jdbcTemplate.update("DELETE FROM daily_word_preferences");
    }

    private WordEntry mapWord(ResultSet rs, int row) throws SQLException {
        String types = rs.getString("word_types");
        return new WordEntry(UUID.fromString(rs.getString("id")), rs.getString("language"),
                rs.getString("word"), List.of(jsonMapper.readValue(
                        rs.getString("english_translations"), String[].class)),
                rs.getString("pronunciation"), rs.getString("pinyin"), rs.getString("level"),
                types.isBlank() ? List.of() : Arrays.asList(types.split("\\|")),
                List.of(jsonMapper.readValue(rs.getString("examples"), WordExample[].class)));
    }
}
