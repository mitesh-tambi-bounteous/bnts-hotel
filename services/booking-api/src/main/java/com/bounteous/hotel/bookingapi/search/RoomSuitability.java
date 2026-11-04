package com.bounteous.hotel.bookingapi.search;

import com.bounteous.hotel.bookingapi.catalogue.RoomType;

/**
 * Capacity rule: guests are shared evenly across the searched rooms, so a room suits the search when
 * it sleeps at least ceil(guests / rooms). A hotel can serve the search when it has at least
 * `rooms` such rooms free.
 */
public final class RoomSuitability {

    private RoomSuitability() {}

    public static int guestsPerRoom(int guests, int rooms) {
        return (guests + rooms - 1) / rooms;
    }

    public static boolean suits(RoomType room, int guests, int rooms) {
        return room.sleeps() >= guestsPerRoom(guests, rooms);
    }

    public static String unsuitableReason(RoomType room, int guests, int rooms) {
        return "Doesn't suit your search: sleeps " + room.sleeps() + ", and you're searching " + guests
                + (guests == 1 ? " guest" : " guests") + " in " + rooms + (rooms == 1 ? " room." : " rooms.");
    }
}
