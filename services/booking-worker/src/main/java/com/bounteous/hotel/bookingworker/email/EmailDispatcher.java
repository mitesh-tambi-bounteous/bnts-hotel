package com.bounteous.hotel.bookingworker.email;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends outbox emails with at-least-once delivery: a row is marked SENT only after the sender accepted it,
 * so a crash in between can send a duplicate but never lose a confirmation.
 */
@Component
public class EmailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final OutboxStore store;
    private final EmailSender sender;
    private final ConfirmationEmailTemplate template;
    private final Clock clock;
    private final int batchSize;
    private final int leaseSeconds;
    private final int maxAttempts;
    private final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public EmailDispatcher(
            OutboxStore store,
            EmailSender sender,
            ConfirmationEmailTemplate template,
            @Value("${email.batch-size:10}") int batchSize,
            @Value("${email.lease-seconds:120}") int leaseSeconds,
            @Value("${email.max-attempts:5}") int maxAttempts) {
        this(store, sender, template, Clock.systemUTC(), batchSize, leaseSeconds, maxAttempts);
    }

    EmailDispatcher(
            OutboxStore store,
            EmailSender sender,
            ConfirmationEmailTemplate template,
            Clock clock,
            int batchSize,
            int leaseSeconds,
            int maxAttempts) {
        this.store = store;
        this.sender = sender;
        this.template = template;
        this.clock = clock;
        this.batchSize = batchSize;
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${email.poll-interval-ms:10000}")
    public void dispatchDue() {
        for (var row : store.claimDue(batchSize, leaseSeconds)) {
            dispatch(row);
        }
    }

    private void dispatch(OutboxStore.Row row) {
        try {
            var payload = mapper.readValue(row.payload(), ConfirmationPayload.class);
            sender.send(template.render(row.toAddress(), payload));
            store.markSent(row.id());
        } catch (Exception e) {
            int attempts = row.attempts() + 1;
            String error = String.valueOf(e.getMessage());
            if (attempts >= maxAttempts) {
                log.error("Email {} failed permanently after {} attempts: {}", row.id(), attempts, error);
                store.markFailed(row.id(), attempts, error);
            } else {
                Instant next = clock.instant().plus(backoff(attempts));
                log.warn("Email {} attempt {} failed, retrying at {}: {}", row.id(), attempts, next, error);
                store.markRetry(row.id(), attempts, next, error);
            }
        }
    }

    /** 1, 2, 4, 8 minutes after attempts 1..4. */
    static Duration backoff(int attempts) {
        return Duration.ofMinutes(1L << (attempts - 1));
    }
}
