package com.berdachuk.meteoris.insight.memory;

import com.berdachuk.meteoris.insight.core.IdGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Lightweight session metadata and lifecycle service.
 *
 * <p>Sessions are durable across turns. This service tracks creation, last activity, and
 * compaction state per session. Message storage continues to use the existing Spring AI
 * {@code ChatMemory} / {@code JdbcChatMemoryRepository} pipeline.
 */
@Service
public class SessionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final IdGenerator idGenerator;

    public SessionService(NamedParameterJdbcTemplate jdbc, IdGenerator idGenerator) {
        this.jdbc = jdbc;
        this.idGenerator = idGenerator;
    }

    public Session createSession(String conversationId) {
        String sid = idGenerator.generateId();
        var params = new MapSqlParameterSource()
                .addValue("id", sid)
                .addValue("conversation_id", conversationId)
                .addValue("created_at", java.sql.Timestamp.from(Instant.now()))
                .addValue("updated_at", java.sql.Timestamp.from(Instant.now()));
        jdbc.update(
                "INSERT INTO ai_session (id, conversation_id, created_at, updated_at) VALUES (:id, :conversation_id, :created_at, :updated_at)",
                params);
        return new Session(sid, conversationId, Instant.now(), Instant.now(), Optional.empty());
    }

    public Optional<Session> findById(String id) {
        try {
            Session s = jdbc.queryForObject(
                    "SELECT id, conversation_id, created_at, updated_at, compacted_at FROM ai_session WHERE id = :id",
                    new MapSqlParameterSource("id", id),
                    MAPPER);
            return Optional.ofNullable(s);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public void touch(String id) {
        jdbc.update(
                "UPDATE ai_session SET updated_at = :now WHERE id = :id",
                Map.of("now", java.sql.Timestamp.from(Instant.now()), "id", id));
    }

    public void recordCompaction(String id) {
        jdbc.update(
                "UPDATE ai_session SET compacted_at = :now WHERE id = :id",
                Map.of("now", java.sql.Timestamp.from(Instant.now()), "id", id));
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM ai_session WHERE id = :id", Map.of("id", id));
    }

    private static final RowMapper<Session> MAPPER = (rs, rowNum) -> new Session(
            rs.getString("id"),
            rs.getString("conversation_id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            Optional.ofNullable(rs.getTimestamp("compacted_at")).map(java.sql.Timestamp::toInstant));

    public record Session(
            String id, String conversationId, Instant createdAt, Instant updatedAt, Optional<Instant> compactedAt) {}
}
