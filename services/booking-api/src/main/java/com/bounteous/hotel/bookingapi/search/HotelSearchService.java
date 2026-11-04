package com.bounteous.hotel.bookingapi.search;

import com.bounteous.hotel.bookingapi.booking.NotFoundException;
import com.bounteous.hotel.bookingapi.catalogue.CatalogueReader;
import com.bounteous.hotel.bookingapi.catalogue.Hotel;
import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import com.bounteous.hotel.bookingapi.search.HotelDetail.RoomView;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class HotelSearchService {

    private final CatalogueReader catalogue;

    public HotelSearchService(CatalogueReader catalogue) {
        this.catalogue = catalogue;
    }

    /** The brand's hotels in the city whose free, suitable rooms can cover the searched rooms. */
    public List<HotelSummary> search(SearchQuery query) {
        return catalogue.hotelsInCity(query.city()).stream()
                .map(hotel -> summarise(hotel, query))
                .flatMap(Optional::stream)
                .toList();
    }

    public HotelDetail detail(long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, int rooms) {
        SearchQuery.validateStay(checkIn, checkOut, guests, rooms);
        Hotel hotel = catalogue.hotel(hotelId).orElseThrow(() -> new NotFoundException("Hotel not found"));
        Map<Long, Integer> free = catalogue.availability(hotelId, checkIn, checkOut);
        List<RoomView> views = catalogue.roomTypes(hotelId).stream()
                .map(room -> view(room, free.getOrDefault(room.id(), 0), guests, rooms))
                .toList();
        return new HotelDetail(
                hotel.id(), hotel.name(), hotel.city(), hotel.summary(), catalogue.tabs(hotelId), views);
    }

    private Optional<HotelSummary> summarise(Hotel hotel, SearchQuery query) {
        Map<Long, Integer> free = catalogue.availability(hotel.id(), query.checkIn(), query.checkOut());
        List<RoomType> suitable = catalogue.roomTypes(hotel.id()).stream()
                .filter(room -> free.getOrDefault(room.id(), 0) > 0)
                .filter(room -> RoomSuitability.suits(room, query.guests(), query.rooms()))
                .toList();
        int freeRooms = suitable.stream().mapToInt(room -> free.get(room.id())).sum();
        if (freeRooms < query.rooms()) {
            return Optional.empty();
        }
        long from = suitable.stream().mapToLong(RoomType::pricePerNightPaise).min().orElseThrow();
        return Optional.of(new HotelSummary(hotel.id(), hotel.name(), hotel.summary(), from));
    }

    private RoomView view(RoomType room, int free, int guests, int rooms) {
        if (free == 0) {
            return roomView(room, RoomStatus.SOLD_OUT, "No rooms left for your dates.");
        }
        if (!RoomSuitability.suits(room, guests, rooms)) {
            return roomView(room, RoomStatus.UNSUITABLE, RoomSuitability.unsuitableReason(room, guests, rooms));
        }
        return roomView(room, RoomStatus.AVAILABLE, null);
    }

    private RoomView roomView(RoomType room, RoomStatus status, String reason) {
        return new RoomView(
                room.id(), room.name(), room.bed(), room.sleeps(), room.pricePerNightPaise(), status, reason);
    }
}
