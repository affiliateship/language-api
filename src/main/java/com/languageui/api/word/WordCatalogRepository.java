package com.languageui.api.word;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class WordCatalogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public WordCatalogRepository(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void save(WordEntry word) {
        jdbcTemplate.update("""
                INSERT INTO word_entries
                    (id, language, word, english_translations, pronunciation, pinyin, level,
                     word_types, examples)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                word.id().toString(), word.language(), word.word(),
                jsonMapper.writeValueAsString(word.englishTranslation()),
                word.pronunciation(), word.pinyin(), word.level(), String.join("|", word.wordTypes()),
                jsonMapper.writeValueAsString(word.examples()));
    }

    public boolean exists(String language, String word, String pinyin) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM word_entries WHERE language = ? AND word = ? AND pinyin = ?
                """, Integer.class, language, word, pinyin);
        return count != null && count > 0;
    }

    public List<WordEntry> findByLanguage(String language) {
        return jdbcTemplate.query("SELECT * FROM word_entries WHERE language = ?", this::map, language);
    }

    public Optional<WordEntry> findById(UUID id) {
        List<WordEntry> matches = jdbcTemplate.query(
                "SELECT * FROM word_entries WHERE id = ?", this::map, id.toString());
        return matches.stream().findFirst();
    }

    public List<WordEntry> findExact(String language, String text) {
        return jdbcTemplate.query("""
                SELECT * FROM word_entries WHERE language = ? AND LOWER(word) = LOWER(?)
                ORDER BY level, word
                """, this::map, language, text);
    }

    public long countByLanguage(String language) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM word_entries WHERE language = ?", Long.class, language);
        return count == null ? 0 : count;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM daily_word_deliveries");
        jdbcTemplate.update("DELETE FROM topic_words");
        jdbcTemplate.update("DELETE FROM word_entries");
    }

    private WordEntry map(ResultSet resultSet, int rowNumber) throws SQLException {
        String types = resultSet.getString("word_types");
        return new WordEntry(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("language"),
                resultSet.getString("word"),
                List.of(jsonMapper.readValue(resultSet.getString("english_translations"), String[].class)),
                resultSet.getString("pronunciation"),
                resultSet.getString("pinyin"),
                resultSet.getString("level"),
                types.isBlank() ? List.of() : Arrays.asList(types.split("\\|")),
                List.of(jsonMapper.readValue(resultSet.getString("examples"), WordExample[].class)));
    }
}
