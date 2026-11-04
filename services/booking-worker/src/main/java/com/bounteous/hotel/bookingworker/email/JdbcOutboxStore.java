package com.bounteous.hotel.bookingworker.email;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxStore implements OutboxStore {
    private final JdbcTemplate jdbc;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Row> claimDue(int limit, int leaseSeconds) {
        return jdbc.query(
                """
                UPDATE email_outbox
                   SET next_attempt_at = now() + make_interval(secs => ?)
                 WHERE id IN (SELECT id FROM email_outbox
                               WHERE status = 'PENDING' AND next_attempt_at <= now()
                               ORDER BY next_attempt_at
                               LIMIT ?
                               FOR UPDATE SKIP LOCKED)
                RETURNING id, to_address, payload::text AS payload, attempts
                """,
                (rs, i) -> new Row(rs.getLong("id"), rs.getString("to_address"), rs.getString("payload"), rs.getInt("attempts")),
                leaseSeconds, limit);
    }

    @Override
    public void markSent(long id) {
        jdbc.update("UPDATE email_outbox SET status = 'SENT', sent_at = now(), last_error = NULL WHERE id = ?", id);
    }

    @Override
    public void markRetry(long id, int attempts, Instant nextAttemptAt, String error) {
        jdbc.update(
                "UPDATE email_outbox SET attempts = ?, next_attempt_at = ?, last_error = ? WHERE id = ?",
                attempts, Timestamp.from(nextAttemptAt), error, id);
    }

    @Override
    public void markFailed(long id, int attempts, String error) {
        jdbc.update("UPDATE email_outbox SET status = 'FAILED', attempts = ?, last_error = ? WHERE id = ?", attempts, error, id);
    }
}
