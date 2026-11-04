package com.bounteous.hotel.bookingapi.account;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GuestAccountRepository {

    /** The account holding a booking, and whether this checkout created it. */
    public record AccountRef(long id, boolean created) {}

    private final JdbcTemplate jdbc;

    public GuestAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Finds the account for a lower-cased email, or creates it. Safe under concurrent checkouts. */
    public AccountRef findOrCreate(String email) {
        return jdbc.query(
                        "INSERT INTO guest_account (email) VALUES (?) ON CONFLICT (email) DO NOTHING RETURNING id",
                        (rs, i) -> new AccountRef(rs.getLong("id"), true),
                        email)
                .stream()
                .findFirst()
                .orElseGet(() -> new AccountRef(
                        jdbc.queryForObject("SELECT id FROM guest_account WHERE email = ?", Long.class, email),
                        false));
    }
}
