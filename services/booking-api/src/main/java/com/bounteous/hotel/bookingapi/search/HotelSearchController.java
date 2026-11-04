package com.bounteous.hotel.bookingapi.search;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelSearchController {

    private final HotelSearchService service;

    public HotelSearchController(HotelSearchService service) {
        this.service = service;
    }

    @GetMapping("/hotels")
    public List<HotelSummary> search(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam int rooms) {
        return service.search(new SearchQuery(city, checkIn, checkOut, guests, rooms));
    }

    @GetMapping("/hotels/{id}")
    public HotelDetail detail(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam int rooms) {
        return service.detail(id, checkIn, checkOut, guests, rooms);
    }
}
