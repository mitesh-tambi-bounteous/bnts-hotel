package com.bounteous.hotel.bookingapi.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class BookingReferenceGeneratorTest {

    private final BookingReferenceGenerator generator = new BookingReferenceGenerator();

    @Test
    void referencesLookLikeHbPlusSixCharacters() { // AC11
        for (int i = 0; i < 200; i++) {
            assertThat(generator.next()).matches("^HB-[A-Z0-9]{6}$");
        }
    }

    @Test
    void referencesVary() {
        var seen = new HashSet<String>();
        for (int i = 0; i < 100; i++) {
            seen.add(generator.next());
        }
        assertThat(seen.size()).isGreaterThan(90);
    }
}
