package com.bounteous.hotel.bookingworker.email;

public class EmailSendException extends Exception {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
