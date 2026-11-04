package com.bounteous.hotel.bookingapi.catalogue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read port over the catalogue tables, so search logic can be tested without a database. */
public interface CatalogueReader {

    List<Hotel> hotelsInCity(String city);

    Optional<Hotel> hotel(long hotelId);

    /** Only the tabs the hotel has a row for, in display order. */
    List<Tab> tabs(long hotelId);

    List<RoomType> roomTypes(long hotelId);

    /**
     * Rooms free on every night of [checkIn, checkOut) per room type of the hotel; a room type with
     * a missing night has none.
     */
    Map<Long, Integer> availability(long hotelId, LocalDate checkIn, LocalDate checkOut);
}
