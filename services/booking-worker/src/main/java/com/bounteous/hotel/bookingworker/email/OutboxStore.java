package com.bounteous.hotel.bookingworker.email;

import java.time.Instant;
import java.util.List;

/** Access to email_outbox. There is deliberately no delete operation. */
public interface OutboxStore {

    record Row(long id, String toAddress, String payload, int attempts) {}

    /** Claims due PENDING rows and leases them: they are not returned again until the lease expires. */
    List<Row> claimDue(int limit, int leaseSeconds);

    void markSent(long id);

    /** Records a failed attempt and schedules the next one. */
    void markRetry(long id, int attempts, Instant nextAttemptAt, String error);

    void markFailed(long id, int attempts, String error);
}
