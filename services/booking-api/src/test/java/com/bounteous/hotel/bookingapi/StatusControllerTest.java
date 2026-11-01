package com.bounteous.hotel.bookingapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatusControllerTest {
    @Test
    void reportsUp() {
        assertEquals("UP", new StatusController().status().get("status"));
    }
}
