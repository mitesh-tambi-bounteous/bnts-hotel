package com.bounteous.hotel.bookingapi.email;

import java.util.List;

/**
 * Snapshot the Booking Worker renders the confirmation email from, so it needs no joins. The same
 * JSON is read by the worker's own ConfirmationEmailPayload: keep both in step (see the fixture
 * test in each project). Dates are ISO yyyy-MM-dd; amounts are paise, taxes included.
 */
public record ConfirmationEmailPayload(
        String firstName,
        String reference,
        String hotelName,
        String checkIn,
        String checkOut,
        long nights,
        List<RoomLine> rooms,
        long totalPaise) {

    public record RoomLine(String name, long amountPaise) {}
}
