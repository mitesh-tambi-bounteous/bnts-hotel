package com.bounteous.hotel.bookingapi.catalogue;

public record RoomType(
        long id, long hotelId, String name, String bed, int sleeps, long pricePerNightPaise) {}
