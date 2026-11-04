package com.bounteous.hotel.bookingworker.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ConfirmationEmailTemplateTest {

    private ConfirmationPayload fixture() throws IOException {
        try (var in = getClass().getResourceAsStream("/confirmation-email-payload.json")) {
            return new ObjectMapper().readValue(in, ConfirmationPayload.class);
        }
    }

    @Test
    void theEmailCarriesTheReferenceHotelDatesRoomsAndTotal() throws IOException { // AC13
        var message = new ConfirmationEmailTemplate().render("asha@example.com", fixture());

        assertThat(message.to()).isEqualTo("asha@example.com");
        assertThat(message.subject()).contains("HB-7K2M9Q").contains("Harbour View Mumbai");
        assertThat(message.text())
                .contains("Hi Asha")
                .contains("HB-7K2M9Q")
                .contains("2026-12-12")
                .contains("2026-12-14")
                .contains("Deluxe sea-facing room: Rs 15,600")
                .contains("Executive room: Rs 19,200")
                .contains("Total paid: Rs 34,800");
    }

    @Test
    void paiseAreShownWhenNotWhole() {
        assertThat(ConfirmationEmailTemplate.rupees(1_560_050)).isEqualTo("Rs 15,600.50");
    }
}
