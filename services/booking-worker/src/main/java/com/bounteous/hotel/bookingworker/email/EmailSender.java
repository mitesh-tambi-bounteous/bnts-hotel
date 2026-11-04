package com.bounteous.hotel.bookingworker.email;

public interface EmailSender {
    void send(EmailMessage message) throws EmailSendException;
}
