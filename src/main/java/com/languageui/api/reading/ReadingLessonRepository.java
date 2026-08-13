package com.languageui.api.reading;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReadingLessonRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReadingLessonRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void save(ReadingLesson lesson) {
        jdbcTemplate.update("""
                INSERT INTO reading_lessons
                    (id, language, level, lesson_type, title, original_text, english_translation)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, lesson.id().toString(), lesson.language(), lesson.level(),
                lesson.lessonType().name(), lesson.title(), lesson.originalText(),
                lesson.englishTranslation());
        for (int sequence = 0; sequence < lesson.keyWords().size(); sequence++) {
            ReadingWord word = lesson.keyWords().get(sequence);
            jdbcTemplate.update("""
                    INSERT INTO reading_words
                        (id, lesson_id, sequence_number, original, english_translation,
                         pronunciation, start_index, end_index)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), lesson.id().toString(), sequence,
                    word.original(), word.englishTranslation(), word.pronunciation(),
                    word.startIndex(), word.endIndex());
        }
    }

    public List<ReadingLesson> findAll(String language) {
        String sql = "SELECT * FROM reading_lessons" +
                (language == null ? "" : " WHERE language = ?") + " ORDER BY created_at, title";
        return language == null ? jdbcTemplate.query(sql, this::map)
                : jdbcTemplate.query(sql, this::map, language);
    }

    public Optional<ReadingLesson> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM reading_lessons WHERE id = ?", this::map,
                id.toString()).stream().findFirst();
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM reading_words");
        jdbcTemplate.update("DELETE FROM reading_lessons");
    }

    private ReadingLesson map(ResultSet rs, int row) throws SQLException {
        UUID lessonId = UUID.fromString(rs.getString("id"));
        List<ReadingWord> words = jdbcTemplate.query("""
                SELECT * FROM reading_words WHERE lesson_id = ? ORDER BY sequence_number
                """, (wordRs, wordRow) -> new ReadingWord(wordRs.getString("original"),
                wordRs.getString("english_translation"), wordRs.getString("pronunciation"),
                wordRs.getInt("start_index"), wordRs.getInt("end_index")), lessonId.toString());
        return new ReadingLesson(lessonId, rs.getString("language"), rs.getString("level"),
                LessonType.valueOf(rs.getString("lesson_type")), rs.getString("title"),
                rs.getString("original_text"),
                rs.getString("english_translation"), words);
    }
}
