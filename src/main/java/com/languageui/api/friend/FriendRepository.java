package com.languageui.api.friend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FriendRepository {
    private final JdbcTemplate jdbcTemplate;

    public FriendRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public boolean requestExists(UUID first, UUID second) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM friend_requests
                WHERE status = 'PENDING' AND ((requester_id = ? AND recipient_id = ?)
                    OR (requester_id = ? AND recipient_id = ?))
                """, Integer.class, first.toString(), second.toString(), second.toString(), first.toString());
        return count != null && count > 0;
    }

    public boolean areFriends(UUID first, UUID second) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?",
                Integer.class, first.toString(), second.toString());
        return count != null && count > 0;
    }

    public PendingRequest createRequest(UUID requester, UUID recipient) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO friend_requests (id, requester_id, recipient_id, status)
                VALUES (?, ?, ?, 'PENDING')
                """, id.toString(), requester.toString(), recipient.toString());
        return findRequest(id).orElseThrow();
    }

    public Optional<PendingRequest> findRequest(UUID id) {
        return jdbcTemplate.query("SELECT * FROM friend_requests WHERE id = ?", (rs, row) ->
                new PendingRequest(UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("requester_id")),
                        UUID.fromString(rs.getString("recipient_id")), rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()), id.toString()).stream().findFirst();
    }

    public List<PendingRequest> incoming(UUID recipient) {
        return jdbcTemplate.query("""
                SELECT * FROM friend_requests
                WHERE recipient_id = ? AND status = 'PENDING' ORDER BY created_at
                """, (rs, row) -> new PendingRequest(UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("requester_id")),
                        UUID.fromString(rs.getString("recipient_id")), rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()), recipient.toString());
    }

    public void accept(PendingRequest request) {
        jdbcTemplate.update("UPDATE friend_requests SET status = 'ACCEPTED' WHERE id = ?",
                request.id().toString());
        jdbcTemplate.update("INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)",
                request.requesterId().toString(), request.recipientId().toString());
        jdbcTemplate.update("INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)",
                request.recipientId().toString(), request.requesterId().toString());
    }

    public void deleteRequest(UUID id) {
        jdbcTemplate.update("DELETE FROM friend_requests WHERE id = ?", id.toString());
    }

    public List<UUID> friendIds(UUID userId) {
        return jdbcTemplate.query("SELECT friend_id FROM friendships WHERE user_id = ? ORDER BY created_at",
                (rs, row) -> UUID.fromString(rs.getString(1)), userId.toString());
    }

    public void removeFriend(UUID first, UUID second) {
        jdbcTemplate.update("""
                DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?)
                    OR (user_id = ? AND friend_id = ?)
                """, first.toString(), second.toString(), second.toString(), first.toString());
    }

    record PendingRequest(UUID id, UUID requesterId, UUID recipientId, String status,
                          LocalDateTime createdAt) { }
}
