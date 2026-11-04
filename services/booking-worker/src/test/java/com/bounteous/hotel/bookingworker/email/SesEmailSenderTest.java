package com.bounteous.hotel.bookingworker.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

class SesEmailSenderTest {

    private final SesV2Client client = mock(SesV2Client.class);
    private final SesEmailSender sender = new SesEmailSender(client, "bookings@example.com");

    @Test
    void sendsFromTheConfiguredAddressToTheGuest() throws Exception {
        when(client.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().build());

        sender.send(new EmailMessage("asha@example.com", "Subject", "Body"));

        var captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(client).sendEmail(captor.capture());
        assertThat(captor.getValue().fromEmailAddress()).isEqualTo("bookings@example.com");
        assertThat(captor.getValue().destination().toAddresses()).containsExactly("asha@example.com");
        assertThat(captor.getValue().content().simple().subject().data()).isEqualTo("Subject");
    }

    @Test
    void anSesErrorBecomesAnEmailSendException() {
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(SesV2Exception.builder().message("throttled").build());

        assertThatThrownBy(() -> sender.send(new EmailMessage("asha@example.com", "S", "B")))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("throttled");
    }
}
