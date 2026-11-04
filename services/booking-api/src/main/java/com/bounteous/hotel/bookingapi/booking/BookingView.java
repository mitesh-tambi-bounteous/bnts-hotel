package com.bounteous.hotel.bookingapi.booking;

import java.time.LocalDate;
import java.util.List;

/** The confirmation view of a booking, all amounts in paise with taxes included. */
public record BookingView(
        String reference,
        String hotelName,
        LocalDate checkIn,
        LocalDate checkOut,
        long nights,
        List<RoomLine> rooms,
        long totalPaise,
        String email,
        String guestName,
        boolean accountCreated) {

    public record RoomLine(String name, long pricePerNightPaise, long totalPaise) {}
}
