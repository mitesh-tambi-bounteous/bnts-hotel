package com.bounteous.hotel.bookingapi.booking;

/** A selected room is no longer available for the stay. */
public class RoomUnavailableException extends RuntimeException {

    private final long roomTypeId;

    public RoomUnavailableException(long roomTypeId) {
        super("Room type " + roomTypeId + " is no longer available");
        this.roomTypeId = roomTypeId;
    }

    public long roomTypeId() {
        return roomTypeId;
    }
}
