package com.bounteous.hotel.bookingapi.booking;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates references like HB-7K2M9Q from an alphabet without look-alike characters. */
@Component
public class BookingReferenceGenerator {

    /** Letters and digits minus look-alikes: I, L, O, 0 and 1. */
    private static final String ALPHABET = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789")
            .replaceAll("[ILO01]", "");
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder reference = new StringBuilder("HB-");
        for (int i = 0; i < LENGTH; i++) {
            reference.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return reference.toString();
    }
}
