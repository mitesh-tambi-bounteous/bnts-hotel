package com.bounteous.hotel.bookingapi.payment;

/** The payment proof is invalid, not captured, or for a different amount; nothing was booked. */
public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException(String message) {
        super(message);
    }
}
