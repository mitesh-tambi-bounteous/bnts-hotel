package com.bounteous.hotel.bookingapi.search;

import com.bounteous.hotel.bookingapi.catalogue.CatalogueReader;
import com.bounteous.hotel.bookingapi.catalogue.Hotel;
import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import com.bounteous.hotel.bookingapi.catalogue.Tab;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The prototype's Mumbai hotels in memory. */
public class FakeCatalogue implements CatalogueReader {

    public static final long HARBOUR = 1;
    public static final long BANDRA = 2;
    public static final long AIRPORT = 3;
    public static final long MARINE = 4;
    public static final long DELUXE = 101;
    public static final long EXECUTIVE = 102;
    public static final long SUITE = 103;
    public static final long COMPACT = 104;

    private final List<Hotel> hotels = List.of(
            new Hotel(HARBOUR, "Harbour View Mumbai", "Mumbai", "Colaba, sea-facing rooms"),
            new Hotel(BANDRA, "Bandra Court", "Mumbai", "Bandra West, near the sea link"),
            new Hotel(AIRPORT, "Airport Inn Andheri", "Mumbai", "Ten minutes from the airport"),
            new Hotel(MARINE, "Marine Drive Residency", "Mumbai", "Marine Drive, promenade views"));

    private final List<RoomType> rooms = List.of(
            new RoomType(DELUXE, HARBOUR, "Deluxe sea-facing room", "1 king bed", 2, 780_000),
            new RoomType(EXECUTIVE, HARBOUR, "Executive room", "2 twin beds", 3, 960_000),
            new RoomType(SUITE, HARBOUR, "Harbour suite", "1 king bed and sofa bed", 4, 1_420_000),
            new RoomType(COMPACT, HARBOUR, "Compact single room", "1 single bed", 1, 490_000),
            new RoomType(201, BANDRA, "Standard double room", "1 queen bed", 2, 640_000),
            new RoomType(202, BANDRA, "Family room", "2 queen beds", 4, 820_000),
            new RoomType(301, AIRPORT, "Standard room", "2 single beds", 3, 420_000),
            new RoomType(401, MARINE, "Premium sea-view room", "1 king bed", 3, 910_000));

    /** Free rooms per room type for every night. */
    private final Map<Long, Integer> free = new HashMap<>();

    public FakeCatalogue() {
        free.put(DELUXE, 4);
        free.put(EXECUTIVE, 3);
        free.put(SUITE, 0);
        free.put(COMPACT, 3);
        free.put(201L, 5);
        free.put(202L, 3);
        free.put(301L, 2);
        free.put(401L, 1);
    }

    public void setFree(long roomTypeId, int available) {
        free.put(roomTypeId, available);
    }

    @Override
    public List<Hotel> hotelsInCity(String city) {
        return hotels.stream().filter(h -> h.city().equalsIgnoreCase(city)).toList();
    }

    @Override
    public Optional<Hotel> hotel(long hotelId) {
        return hotels.stream().filter(h -> h.id() == hotelId).findFirst();
    }

    @Override
    public List<Tab> tabs(long hotelId) {
        if (hotelId == HARBOUR) {
            return List.of(new Tab("Overview", "o"), new Tab("Amenities", "a"), new Tab("Location", "l"));
        }
        return List.of(new Tab("Overview", "o"));
    }

    @Override
    public List<RoomType> roomTypes(long hotelId) {
        return rooms.stream().filter(r -> r.hotelId() == hotelId).toList();
    }

    @Override
    public Map<Long, Integer> availability(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        Map<Long, Integer> result = new HashMap<>();
        roomTypes(hotelId).forEach(r -> result.put(r.id(), free.getOrDefault(r.id(), 0)));
        return result;
    }
}
