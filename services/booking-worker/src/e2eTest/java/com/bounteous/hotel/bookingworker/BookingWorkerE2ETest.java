package com.bounteous.hotel.bookingworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bounteous.hotel.bookingworker.email.EmailDispatcher;
import com.bounteous.hotel.bookingworker.email.JdbcOutboxStore;
import com.bounteous.hotel.bookingworker.email.OutboxStore;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Runs against the Booking API's real V1 schema; the email.* properties keep the poller out of the way. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"email.poll-interval-ms=3600000", "email.from-address="})
class BookingWorkerE2ETest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ResourceDatabasePopulator(new FileSystemResource(
                        Path.of("../booking-api/src/main/resources/db/migration/V1__booking_schema.sql")))
                .execute(ds);
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String PAYLOAD = """
            {"firstName":"Asha","reference":"HB-7K2M9Q","hotelName":"Harbour View Mumbai","checkIn":"2026-12-12",
             "checkOut":"2026-12-14","nights":2,"rooms":[{"name":"Deluxe sea-facing room","amountPaise":1560000}],
             "totalPaise":1560000}""";

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmailDispatcher dispatcher;
    @Autowired JdbcOutboxStore store;

    @BeforeEach
    void clearOutbox() {
        // test isolation only: this is the test database, not application behaviour
        jdbc.execute("TRUNCATE email_outbox");
    }

    private void enqueue(String to) {
        jdbc.update("INSERT INTO email_outbox (to_address, payload) VALUES (?, ?::jsonb)", to, PAYLOAD);
    }

    @Test
    void statusEndpointIsUp() {
        assertEquals(200, rest.getForEntity("/status", String.class).getStatusCode().value());
    }

    @Test
    void aPendingRowIsSentAndMarkedSent() { // AC13
        enqueue("asha@example.com");

        dispatcher.dispatchDue();

        assertThat(jdbc.queryForObject("SELECT status FROM email_outbox", String.class)).isEqualTo("SENT");
        assertThat(jdbc.queryForObject("SELECT sent_at IS NOT NULL FROM email_outbox", Boolean.class)).isTrue();
    }

    @Test
    void aRowNotYetDueIsLeftAlone() {
        enqueue("asha@example.com");
        jdbc.update("UPDATE email_outbox SET next_attempt_at = now() + interval '1 hour'");

        dispatcher.dispatchDue();

        assertThat(jdbc.queryForObject("SELECT status FROM email_outbox", String.class)).isEqualTo("PENDING");
    }

    @Test
    void twoWorkersNeverClaimTheSameRowAndALeasedRowIsNotReclaimedEarly() throws Exception { // AC17
        for (int i = 0; i < 20; i++) {
            enqueue("guest" + i + "@example.com");
        }
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<List<OutboxStore.Row>>> claims =
                    List.of(() -> store.claimDue(15, 120), () -> store.claimDue(15, 120));
            var results = pool.invokeAll(claims);
            var ids = new java.util.ArrayList<Long>();
            for (var f : results) {
                f.get().forEach(r -> ids.add(r.id()));
            }
            assertThat(ids).doesNotHaveDuplicates().hasSize(20);
        } finally {
            pool.shutdown();
        }
        assertThat(store.claimDue(15, 120)).isEmpty();
    }
}
