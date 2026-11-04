package com.bounteous.hotel.bookingapi.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/** The same fixture is read by the Booking Worker's own payload test: the JSON is a contract. */
class ConfirmationEmailPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsTheSharedFixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/confirmation-email-payload.json")) {
            var payload = mapper.readValue(in, ConfirmationEmailPayload.class);

            assertThat(payload.reference()).isEqualTo("HB-7K2M9Q");
            assertThat(payload.rooms()).hasSize(2);
            assertThat(payload.totalPaise()).isEqualTo(3_480_000L);
        }
    }

    @Test
    void roundTripsToTheSameJson() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/confirmation-email-payload.json")) {
            var original = mapper.readTree(in.readAllBytes());
            var payload = mapper.treeToValue(original, ConfirmationEmailPayload.class);

            // Re-parse the serialised text so int/long node types do not matter.
            var roundTripped = mapper.readTree(mapper.writeValueAsString(payload));
            assertThat((Object) roundTripped).isEqualTo(original);
        }
    }
}
