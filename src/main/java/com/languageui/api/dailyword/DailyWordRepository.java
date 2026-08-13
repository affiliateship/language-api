package com.languageui.api.dailyword;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.languageui.api.word.WordEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DailyWordRepository {
    private final JdbcTemplate jdbcTemplate;

    public DailyWordRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

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
                historyClause + " ORDER BY w.id LIMIT ?";
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
                rs.getString("word"), rs.getString("english_translation"),
                rs.getString("pronunciation"), rs.getString("pinyin"), rs.getString("level"),
                types.isBlank() ? List.of() : Arrays.asList(types.split("\\|")),
                rs.getString("example"), rs.getString("example_translation"));
    }
}
