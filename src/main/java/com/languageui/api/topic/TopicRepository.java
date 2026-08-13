package com.languageui.api.topic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TopicRepository {

    private final JdbcTemplate jdbcTemplate;

    public TopicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Topic topic) {
        jdbcTemplate.update("INSERT INTO topics (id, language, name, description) VALUES (?, ?, ?, ?)",
                topic.id().toString(), topic.language(), topic.name(), topic.description());
    }

    public Optional<Topic> findById(UUID id) {
        return jdbcTemplate.query("""
                SELECT t.*, COUNT(tw.word_id) AS word_count
                FROM topics t LEFT JOIN topic_words tw ON tw.topic_id = t.id
                WHERE t.id = ? GROUP BY t.id, t.language, t.name, t.description
                """, this::mapTopic, id.toString()).stream().findFirst();
    }

    public List<Topic> findAll(String language) {
        String sql = """
                SELECT t.*, COUNT(tw.word_id) AS word_count
                FROM topics t LEFT JOIN topic_words tw ON tw.topic_id = t.id
                """ + (language == null ? "" : " WHERE t.language = ?") + """
                 GROUP BY t.id, t.language, t.name, t.description ORDER BY t.name
                """;
        return language == null ? jdbcTemplate.query(sql, this::mapTopic)
                : jdbcTemplate.query(sql, this::mapTopic, language);
    }

    public boolean existsByLanguageAndName(String language, String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topics WHERE language = ? AND LOWER(name) = LOWER(?)",
                Integer.class, language, name);
        return count != null && count > 0;
    }

    public void addWord(UUID topicId, UUID wordId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topic_words WHERE topic_id = ? AND word_id = ?",
                Integer.class, topicId.toString(), wordId.toString());
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO topic_words (topic_id, word_id) VALUES (?, ?)",
                    topicId.toString(), wordId.toString());
        }
    }

    public void removeWord(UUID topicId, UUID wordId) {
        jdbcTemplate.update("DELETE FROM topic_words WHERE topic_id = ? AND word_id = ?",
                topicId.toString(), wordId.toString());
    }

    public List<UUID> wordIds(UUID topicId) {
        return jdbcTemplate.query("SELECT word_id FROM topic_words WHERE topic_id = ?",
                (resultSet, row) -> UUID.fromString(resultSet.getString(1)), topicId.toString());
    }

    public void select(UUID userId, UUID topicId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_topics WHERE user_id = ? AND topic_id = ?",
                Integer.class, userId.toString(), topicId.toString());
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO user_topics (user_id, topic_id) VALUES (?, ?)",
                    userId.toString(), topicId.toString());
        }
    }

    public void deselect(UUID userId, UUID topicId) {
        jdbcTemplate.update("DELETE FROM user_topics WHERE user_id = ? AND topic_id = ?",
                userId.toString(), topicId.toString());
    }

    public List<Topic> selectedByUser(UUID userId) {
        return jdbcTemplate.query("""
                SELECT t.*, COUNT(tw.word_id) AS word_count
                FROM user_topics ut JOIN topics t ON t.id = ut.topic_id
                LEFT JOIN topic_words tw ON tw.topic_id = t.id
                WHERE ut.user_id = ?
                GROUP BY t.id, t.language, t.name, t.description, ut.selected_at
                ORDER BY ut.selected_at, t.name
                """, this::mapTopic, userId.toString());
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM user_topics");
        jdbcTemplate.update("DELETE FROM topic_words");
        jdbcTemplate.update("DELETE FROM topics");
    }

    private Topic mapTopic(ResultSet resultSet, int row) throws SQLException {
        return new Topic(UUID.fromString(resultSet.getString("id")), resultSet.getString("language"),
                resultSet.getString("name"), resultSet.getString("description"),
                resultSet.getLong("word_count"));
    }
}
