package com.bounteous.hotel.bookingapi.search;

import com.bounteous.hotel.bookingapi.catalogue.Tab;
import java.util.List;

/** A hotel with only its catalogue-defined tabs and every room marked AVAILABLE, SOLD_OUT or UNSUITABLE. */
public record HotelDetail(long id, String name, String city, String summary, List<Tab> tabs, List<RoomView> rooms) {

    public record RoomView(
            long id,
            String name,
            String bed,
            int sleeps,
            long pricePerNightPaise,
            RoomStatus status,
            String reason) {}
}
