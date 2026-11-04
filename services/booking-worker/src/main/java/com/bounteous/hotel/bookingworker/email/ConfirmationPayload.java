package com.bounteous.hotel.bookingworker.email;

import java.util.List;

/** Mirrors the Booking API's outbox payload; both read src/test/resources/confirmation-email-payload.json. */
public record ConfirmationPayload(
        String firstName,
        String reference,
        String hotelName,
        String checkIn,
        String checkOut,
        int nights,
        List<RoomLine> rooms,
        long totalPaise) {

    public record RoomLine(String name, long amountPaise) {}
}
