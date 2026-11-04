package com.bounteous.hotel.bookingworker.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Used when no SES sender address is configured (local development only). */
public class LoggingEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Email (not sent, no SES sender configured) to={} subject={}\n{}", message.to(), message.subject(), message.text());
    }
}
