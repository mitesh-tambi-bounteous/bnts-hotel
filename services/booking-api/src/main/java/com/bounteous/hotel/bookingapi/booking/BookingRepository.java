package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Bookings, their rooms, payments and the inventory they use. Nothing here deletes rows. */
@Repository
public class BookingRepository {

    private static final int REFERENCE_ATTEMPTS = 5;

    private final JdbcTemplate jdbc;
    private final BookingReferenceGenerator references;

    public BookingRepository(JdbcTemplate jdbc, BookingReferenceGenerator references) {
        this.jdbc = jdbc;
        this.references = references;
    }

    /**
     * Takes one room off every night of the stay, only while one is left. Returns false when any
     * night had none (the caller's transaction then rolls the partial decrement back).
     */
    public boolean reserve(long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        int updated = jdbc.update(
                "UPDATE room_inventory SET available = available - 1 "
                        + "WHERE room_type_id = ? AND night >= ? AND night < ? AND available > 0",
                roomTypeId,
                Date.valueOf(checkIn),
                Date.valueOf(checkOut));
        return updated == ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Records a captured payment; a provider reference can be recorded only once. */
    public long recordPayment(String providerRef, long amountPaise) {
        return jdbc.queryForObject(
                "INSERT INTO payment (provider_ref, amount_paise, status) VALUES (?, ?, 'CAPTURED') RETURNING id",
                Long.class,
                providerRef,
                amountPaise);
    }

    /** Provider refs of CAPTURED payments older than the cutoff that no booking uses. */
    public List<String> findOrphanedPayments(Instant olderThan) {
        return jdbc.queryForList(
                "SELECT p.provider_ref FROM payment p WHERE p.status = 'CAPTURED' AND p.created_at < ? "
                        + "AND NOT EXISTS (SELECT 1 FROM booking b WHERE b.payment_id = p.id) ORDER BY p.id",
                String.class,
                Timestamp.from(olderThan));
    }

    public void markPaymentRefunded(String providerRef) {
        jdbc.update("UPDATE payment SET status = 'REFUNDED' WHERE provider_ref = ?", providerRef);
    }

    /** Inserts the booking under a fresh unique reference, then its rooms, and returns the reference. */
    public String save(NewBooking booking) {
        for (int attempt = 0; attempt < REFERENCE_ATTEMPTS; attempt++) {
            String reference = references.next();
            Long bookingId = jdbc.query(
                            "INSERT INTO booking (reference, account_id, hotel_id, check_in, check_out, guests, "
                                    + "total_paise, payment_id, guest_name, guest_phone, account_created) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                    + "ON CONFLICT (reference) DO NOTHING RETURNING id",
                            (rs, i) -> rs.getLong("id"),
                            reference,
                            booking.accountId(),
                            booking.hotelId(),
                            Date.valueOf(booking.checkIn()),
                            Date.valueOf(booking.checkOut()),
                            booking.guests(),
                            booking.totalPaise(),
                            booking.paymentId(),
                            booking.guestName(),
                            booking.guestPhone(),
                            booking.accountCreated())
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (bookingId != null) {
                for (RoomType room : booking.rooms()) {
                    jdbc.update(
                            "INSERT INTO booking_room (booking_id, room_type_id, price_per_night_paise) VALUES (?, ?, ?)",
                            bookingId,
                            room.id(),
                            room.pricePerNightPaise());
                }
                return reference;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }

    public Optional<BookingView> findByReference(String reference) {
        return jdbc.query(
                        "SELECT b.id, b.reference, h.name AS hotel_name, b.check_in, b.check_out, b.total_paise, "
                                + "a.email, b.guest_name, b.account_created "
                                + "FROM booking b JOIN hotel h ON h.id = b.hotel_id "
                                + "JOIN guest_account a ON a.id = b.account_id WHERE b.reference = ?",
                        (rs, i) -> {
                            LocalDate in = rs.getDate("check_in").toLocalDate();
                            LocalDate out = rs.getDate("check_out").toLocalDate();
                            long nights = ChronoUnit.DAYS.between(in, out);
                            List<BookingView.RoomLine> rooms = jdbc.query(
                                    "SELECT r.name, br.price_per_night_paise FROM booking_room br "
                                            + "JOIN room_type r ON r.id = br.room_type_id "
                                            + "WHERE br.booking_id = ? ORDER BY r.id",
                                    (rr, j) -> new BookingView.RoomLine(
                                            rr.getString("name"),
                                            rr.getLong("price_per_night_paise"),
                                            rr.getLong("price_per_night_paise") * nights),
                                    rs.getLong("id"));
                            return new BookingView(
                                    rs.getString("reference"),
                                    rs.getString("hotel_name"),
                                    in,
                                    out,
                                    nights,
                                    rooms,
                                    rs.getLong("total_paise"),
                                    rs.getString("email"),
                                    rs.getString("guest_name"),
                                    rs.getBoolean("account_created"));
                        },
                        reference)
                .stream()
                .findFirst();
    }
}
