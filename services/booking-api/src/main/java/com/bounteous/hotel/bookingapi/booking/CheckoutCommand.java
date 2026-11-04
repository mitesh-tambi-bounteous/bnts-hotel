package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.payment.PaymentProof;
import java.time.LocalDate;
import java.util.List;

public record CheckoutCommand(
        long hotelId,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        List<Long> roomTypeIds,
        String name,
        String email,
        String phone,
        PaymentProof proof) {}
