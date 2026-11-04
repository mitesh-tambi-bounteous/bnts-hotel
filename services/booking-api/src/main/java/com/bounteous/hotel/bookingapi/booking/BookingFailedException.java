package com.bounteous.hotel.bookingapi.booking;

/** The card was charged but the booking could not be saved; the payment has been reversed. */
public class BookingFailedException extends RuntimeException {

    public BookingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
