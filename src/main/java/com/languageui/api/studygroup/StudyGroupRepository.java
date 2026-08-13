package com.languageui.api.studygroup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudyGroupRepository {
    private final JdbcTemplate jdbc;
    public StudyGroupRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void create(StudyGroup group) {
        jdbc.update("""
                INSERT INTO study_groups (id, owner_id, name, language, level, lesson_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, group.id().toString(), group.ownerId().toString(), group.name(), group.language(),
                group.level(), null, group.status().name());
        addMember(group.id(), group.ownerId());
    }
    public Optional<StudyGroup> find(UUID id) {
        return jdbc.query("SELECT * FROM study_groups WHERE id = ?", this::map, id.toString())
                .stream().findFirst();
    }
    public List<StudyGroup> groupsFor(UUID userId) {
        return jdbc.query("""
                SELECT g.* FROM study_groups g JOIN study_group_members m ON m.group_id = g.id
                WHERE m.user_id = ? ORDER BY g.created_at DESC
                """, this::map, userId.toString());
    }
    public boolean isMember(UUID groupId, UUID userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM study_group_members WHERE group_id=? AND user_id=?",
                Integer.class, groupId.toString(), userId.toString());
        return count != null && count > 0;
    }
    public void addMember(UUID groupId, UUID userId) {
        if (!isMember(groupId, userId)) jdbc.update(
                "INSERT INTO study_group_members (group_id,user_id) VALUES (?,?)",
                groupId.toString(), userId.toString());
    }
    public List<UUID> memberIds(UUID groupId) {
        return jdbc.query("SELECT user_id FROM study_group_members WHERE group_id=? ORDER BY joined_at",
                (rs, row) -> UUID.fromString(rs.getString(1)), groupId.toString());
    }
    public StudyGroupInvitation invite(UUID groupId, UUID inviter, UUID invitee, String groupName,
            String inviterName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO study_group_invitations (id,group_id,inviter_id,invitee_id,status)
                VALUES (?,?,?,?,'PENDING')
                """, id.toString(), groupId.toString(), inviter.toString(), invitee.toString());
        return new StudyGroupInvitation(id, groupId, groupName, inviter, inviterName, "PENDING",
                java.time.LocalDateTime.now());
    }
    public Optional<InvitationRow> invitation(UUID id) {
        return jdbc.query("SELECT * FROM study_group_invitations WHERE id=?", (rs,row) ->
                new InvitationRow(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("group_id")),
                        UUID.fromString(rs.getString("inviter_id")), UUID.fromString(rs.getString("invitee_id")),
                        rs.getString("status")), id.toString()).stream().findFirst();
    }
    public List<InvitationRow> invitations(UUID invitee) {
        return jdbc.query("""
                SELECT * FROM study_group_invitations WHERE invitee_id=? AND status='PENDING'
                ORDER BY created_at
                """, (rs,row) -> new InvitationRow(UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("group_id")), UUID.fromString(rs.getString("inviter_id")),
                UUID.fromString(rs.getString("invitee_id")), rs.getString("status")), invitee.toString());
    }
    public void invitationStatus(UUID id, String status) {
        jdbc.update("UPDATE study_group_invitations SET status=? WHERE id=?", status, id.toString());
    }
    public void assignLesson(UUID groupId, UUID lessonId) {
        jdbc.update("UPDATE study_groups SET lesson_id=? WHERE id=?", lessonId.toString(), groupId.toString());
    }
    private StudyGroup map(ResultSet rs, int row) throws SQLException {
        String lesson = rs.getString("lesson_id");
        return new StudyGroup(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("owner_id")),
                rs.getString("name"), rs.getString("language"), rs.getString("level"),
                lesson == null ? null : UUID.fromString(lesson), StudyGroupStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
    record InvitationRow(UUID id, UUID groupId, UUID inviterId, UUID inviteeId, String status) { }
}
