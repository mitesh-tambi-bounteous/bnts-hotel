package com.bounteous.hotel.bookingapi.search;

/** A listed hotel; the price is per night in paise, taxes included. */
public record HotelSummary(long id, String name, String summary, long fromPricePerNightPaise) {}
