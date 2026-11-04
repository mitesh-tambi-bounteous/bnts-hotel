package com.bounteous.hotel.bookingworker.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");

    static class FakeStore implements OutboxStore {
        List<Row> rows = new ArrayList<>();
        List<String> events = new ArrayList<>();
        Instant retryAt;

        @Override
        public List<Row> claimDue(int limit, int leaseSeconds) {
            return rows;
        }

        @Override
        public void markSent(long id) {
            events.add("SENT " + id);
        }

        @Override
        public void markRetry(long id, int attempts, Instant nextAttemptAt, String error) {
            events.add("RETRY " + id + " attempts=" + attempts);
            retryAt = nextAttemptAt;
        }

        @Override
        public void markFailed(long id, int attempts, String error) {
            events.add("FAILED " + id + " attempts=" + attempts);
        }
    }

    private final FakeStore store = new FakeStore();
    private final List<EmailMessage> sent = new ArrayList<>();
    private String payload;
    private boolean failSending;

    @BeforeEach
    void setUp() throws IOException {
        try (var in = getClass().getResourceAsStream("/confirmation-email-payload.json")) {
            payload = new String(in.readAllBytes());
        }
    }

    private EmailDispatcher dispatcher() {
        EmailSender sender = message -> {
            if (failSending) {
                throw new EmailSendException("SES down", new RuntimeException());
            }
            sent.add(message);
        };
        return new EmailDispatcher(store, sender, new ConfirmationEmailTemplate(), Clock.fixed(NOW, ZoneOffset.UTC), 10, 120, 5);
    }

    @Test
    void sendsTheConfirmationAndMarksTheRowSentAfterwards() { // AC13
        store.rows = List.of(new OutboxStore.Row(7, "asha@example.com", payload, 0));

        dispatcher().dispatchDue();

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).to()).isEqualTo("asha@example.com");
        assertThat(store.events).containsExactly("SENT 7");
    }

    @Test
    void aSendFailureSchedulesARetryWithBackoff() { // AC17
        failSending = true;
        store.rows = List.of(new OutboxStore.Row(7, "asha@example.com", payload, 2));

        dispatcher().dispatchDue();

        assertThat(store.events).containsExactly("RETRY 7 attempts=3");
        assertThat(store.retryAt).isEqualTo(NOW.plus(Duration.ofMinutes(4)));
    }

    @Test
    void theFifthFailureMarksTheRowFailed() { // AC17
        failSending = true;
        store.rows = List.of(new OutboxStore.Row(7, "asha@example.com", payload, 4));

        dispatcher().dispatchDue();

        assertThat(store.events).containsExactly("FAILED 7 attempts=5");
    }

    @Test
    void anUnreadablePayloadIsRetriedNotDropped() {
        store.rows = List.of(new OutboxStore.Row(7, "asha@example.com", "not json", 0));

        dispatcher().dispatchDue();

        assertThat(sent).isEmpty();
        assertThat(store.events).containsExactly("RETRY 7 attempts=1");
    }

    @Test
    void backoffDoublesFromOneMinute() {
        assertThat(EmailDispatcher.backoff(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(EmailDispatcher.backoff(4)).isEqualTo(Duration.ofMinutes(8));
    }

    @Test
    void oneBadRowDoesNotStopTheRestOfTheBatch() {
        store.rows = List.of(
                new OutboxStore.Row(1, "a@example.com", "not json", 0),
                new OutboxStore.Row(2, "b@example.com", payload, 0));

        dispatcher().dispatchDue();

        assertThat(store.events).containsExactly("RETRY 1 attempts=1", "SENT 2");
    }
}
