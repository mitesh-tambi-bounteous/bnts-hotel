package com.bounteous.hotel.bookingapi.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Queues the confirmation email. Call inside the booking's transaction so the booking and its
 * email request commit or roll back together; the Booking Worker sends it asynchronously.
 */
@Component
public class EmailOutbox {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public EmailOutbox(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void enqueue(String toAddress, ConfirmationEmailPayload payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the confirmation email", e);
        }
        jdbc.update(
                "INSERT INTO email_outbox (to_address, payload, status) VALUES (?, ?::jsonb, 'PENDING')",
                toAddress,
                json);
    }
}
