package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.catalogue.CatalogueReader;
import com.bounteous.hotel.bookingapi.catalogue.Hotel;
import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import com.bounteous.hotel.bookingapi.search.RoomSuitability;
import com.bounteous.hotel.bookingapi.search.SearchQuery;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Checks the selected rooms are free and computes the total on the server (never from the client). */
@Component
public class StayPricer {

    private final CatalogueReader catalogue;

    public StayPricer(CatalogueReader catalogue) {
        this.catalogue = catalogue;
    }

    /** One entry in {@code rooms} per room booked, in request order. */
    public record Quote(Hotel hotel, long nights, List<RoomType> rooms, long totalPaise) {}

    public Quote price(long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, List<Long> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            throw new ValidationException(Map.of("roomTypeIds", "Select at least one room"));
        }
        SearchQuery.validateStay(checkIn, checkOut, guests, roomTypeIds.size());
        Hotel hotel = catalogue.hotel(hotelId).orElseThrow(() -> new NotFoundException("Hotel not found"));
        Map<Long, RoomType> byId = new HashMap<>();
        catalogue.roomTypes(hotelId).forEach(room -> byId.put(room.id(), room));
        Map<Long, Integer> free = catalogue.availability(hotelId, checkIn, checkOut);

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Map<Long, Integer> wanted = new HashMap<>();
        List<RoomType> rooms = new ArrayList<>();
        long total = 0;
        for (Long id : roomTypeIds) {
            RoomType room = byId.get(id);
            if (room == null) {
                throw new ValidationException(Map.of("roomTypeIds", "Choose rooms from this hotel"));
            }
            if (!RoomSuitability.suits(room, guests, roomTypeIds.size())) {
                throw new ValidationException(Map.of("roomTypeIds", RoomSuitability.unsuitableReason(room, guests, roomTypeIds.size())));
            }
            if (wanted.merge(id, 1, Integer::sum) > free.getOrDefault(id, 0)) {
                throw new RoomUnavailableException(id);
            }
            rooms.add(room);
            total += room.pricePerNightPaise() * nights;
        }
        if (rooms.stream().mapToInt(RoomType::sleeps).sum() < guests) {
            throw new ValidationException(Map.of("roomTypeIds", "The selected rooms don't sleep all your guests"));
        }
        return new Quote(hotel, nights, rooms, total);
    }
}
