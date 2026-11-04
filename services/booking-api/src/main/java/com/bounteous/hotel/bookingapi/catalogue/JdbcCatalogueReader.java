package com.bounteous.hotel.bookingapi.catalogue;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCatalogueReader implements CatalogueReader {

    private static final RowMapper<Hotel> HOTEL = (rs, i) ->
            new Hotel(rs.getLong("id"), rs.getString("name"), rs.getString("city"), rs.getString("summary"));

    private final JdbcTemplate jdbc;

    public JdbcCatalogueReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Hotel> hotelsInCity(String city) {
        return jdbc.query(
                "SELECT id, name, city, summary FROM hotel WHERE lower(city) = lower(?) ORDER BY id", HOTEL, city);
    }

    @Override
    public Optional<Hotel> hotel(long hotelId) {
        return jdbc.query("SELECT id, name, city, summary FROM hotel WHERE id = ?", HOTEL, hotelId).stream()
                .findFirst();
    }

    @Override
    public List<Tab> tabs(long hotelId) {
        return jdbc.query(
                "SELECT name, body FROM hotel_tab WHERE hotel_id = ? ORDER BY position",
                (rs, i) -> new Tab(rs.getString("name"), rs.getString("body")),
                hotelId);
    }

    @Override
    public List<RoomType> roomTypes(long hotelId) {
        return jdbc.query(
                "SELECT id, hotel_id, name, bed, sleeps, price_per_night_paise FROM room_type WHERE hotel_id = ? ORDER BY id",
                (rs, i) -> new RoomType(
                        rs.getLong("id"),
                        rs.getLong("hotel_id"),
                        rs.getString("name"),
                        rs.getString("bed"),
                        rs.getInt("sleeps"),
                        rs.getLong("price_per_night_paise")),
                hotelId);
    }

    @Override
    public Map<Long, Integer> availability(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Map<Long, Integer> result = new HashMap<>();
        for (RoomType room : roomTypes(hotelId)) {
            result.put(room.id(), 0);
        }
        jdbc.query(
                "SELECT i.room_type_id, count(*) AS nights, min(i.available) AS free "
                        + "FROM room_inventory i JOIN room_type r ON r.id = i.room_type_id "
                        + "WHERE r.hotel_id = ? AND i.night >= ? AND i.night < ? GROUP BY i.room_type_id",
                rs -> {
                    if (rs.getLong("nights") == nights) {
                        result.put(rs.getLong("room_type_id"), rs.getInt("free"));
                    }
                },
                hotelId,
                Date.valueOf(checkIn),
                Date.valueOf(checkOut));
        return result;
    }
}
