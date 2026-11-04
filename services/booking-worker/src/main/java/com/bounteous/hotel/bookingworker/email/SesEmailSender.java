package com.bounteous.hotel.bookingworker.email;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

public class SesEmailSender implements EmailSender {
    private final SesV2Client client;
    private final String fromAddress;

    public SesEmailSender(SesV2Client client, String fromAddress) {
        this.client = client;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(EmailMessage message) throws EmailSendException {
        try {
            client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(fromAddress)
                    .destination(Destination.builder().toAddresses(message.to()).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(message.subject()).charset("UTF-8").build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(message.text()).charset("UTF-8").build())
                                            .build())
                                    .build())
                            .build())
                    .build());
        } catch (RuntimeException e) {
            throw new EmailSendException("SES send failed: " + e.getMessage(), e);
        }
    }
}
