package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import java.time.LocalDate;
import java.util.List;

/** Everything needed to insert one booking with all its rooms. */
public record NewBooking(
        long accountId,
        boolean accountCreated,
        long hotelId,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        long totalPaise,
        long paymentId,
        String guestName,
        String guestPhone,
        List<RoomType> rooms) {}
