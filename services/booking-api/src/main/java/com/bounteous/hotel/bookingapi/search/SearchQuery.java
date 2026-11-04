package com.bounteous.hotel.bookingapi.search;

import com.bounteous.hotel.bookingapi.booking.ValidationException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** A guest's search: city, stay dates, number of guests and number of rooms. */
public record SearchQuery(String city, LocalDate checkIn, LocalDate checkOut, int guests, int rooms) {

    static final int MAX_NIGHTS = 30;
    static final int MAX_GUESTS = 30;
    static final int MAX_ROOMS = 9;

    public SearchQuery {
        Map<String, String> errors = stayErrors(checkIn, checkOut, guests, rooms);
        if (city == null || city.isBlank()) {
            errors.put("city", "Choose a city");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Checks the stay part only (used by hotel detail, which has no city). */
    public static void validateStay(LocalDate checkIn, LocalDate checkOut, int guests, int rooms) {
        Map<String, String> errors = stayErrors(checkIn, checkOut, guests, rooms);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static Map<String, String> stayErrors(LocalDate checkIn, LocalDate checkOut, int guests, int rooms) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (checkIn == null) {
            errors.put("checkIn", "Enter a check-in date");
        }
        if (checkOut == null) {
            errors.put("checkOut", "Enter a check-out date");
        } else if (checkIn != null) {
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < 1) {
                errors.put("checkOut", "Choose a check-out date after check-in");
            } else if (nights > MAX_NIGHTS) {
                errors.put("checkOut", "Choose a stay of " + MAX_NIGHTS + " nights or fewer");
            }
        }
        if (guests < 1 || guests > MAX_GUESTS) {
            errors.put("guests", "Choose between 1 and " + MAX_GUESTS + " guests");
        }
        if (rooms < 1 || rooms > MAX_ROOMS) {
            errors.put("rooms", "Choose between 1 and " + MAX_ROOMS + " rooms");
        }
        return errors;
    }
}
