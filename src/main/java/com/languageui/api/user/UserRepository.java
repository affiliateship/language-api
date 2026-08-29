package com.languageui.api.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StoredUser> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY created_at, id", this::mapUser);
    }

    public Optional<StoredUser> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", this::mapUser, id.toString())
                .stream().findFirst();
    }

    public Optional<StoredUser> findByEmail(String email) {
        return jdbcTemplate.query("SELECT * FROM users WHERE email = ?", this::mapUser, email)
                .stream().findFirst();
    }

    public Optional<StoredUser> findByUsername(String username) {
        return jdbcTemplate.query("SELECT * FROM users WHERE username = ?", this::mapUser, username)
                .stream().findFirst();
    }

    public void insert(StoredUser user) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, username, username_changed_at, first_name, last_name, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, user.id().toString(), user.email(), user.username(), user.usernameChangedAt(),
                user.firstName(), user.lastName(), user.passwordHash());
    }

    public void update(StoredUser user) {
        jdbcTemplate.update("""
                UPDATE users SET email = ?, username = ?, username_changed_at = ?,
                    first_name = ?, last_name = ?, password_hash = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, user.email(), user.username(), user.usernameChangedAt(), user.firstName(),
                user.lastName(), user.passwordHash(), user.id().toString());
    }

    public int delete(UUID id) {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id.toString());
    }

    public Set<String> languageCodes(UUID userId) {
        return new LinkedHashSet<>(jdbcTemplate.query("""
                SELECT language_code FROM user_languages WHERE user_id = ? ORDER BY language_code
                """, (rs, row) -> rs.getString("language_code"), userId.toString()));
    }

    public void addLanguage(UUID userId, String languageCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_languages WHERE user_id = ? AND language_code = ?
                """, Integer.class, userId.toString(), languageCode);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO user_languages (user_id, language_code) VALUES (?, ?)",
                    userId.toString(), languageCode);
        }
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM users");
    }

    private StoredUser mapUser(ResultSet rs, int row) throws SQLException {
        java.sql.Timestamp changedAt = rs.getTimestamp("username_changed_at");
        return new StoredUser(UUID.fromString(rs.getString("id")), rs.getString("email"),
                rs.getString("username"), changedAt == null ? null : changedAt.toLocalDateTime(),
                rs.getString("first_name"), rs.getString("last_name"), rs.getString("password_hash"));
    }

    public record StoredUser(UUID id, String email, String username, LocalDateTime usernameChangedAt,
                             String firstName, String lastName, String passwordHash) {
    }
}
