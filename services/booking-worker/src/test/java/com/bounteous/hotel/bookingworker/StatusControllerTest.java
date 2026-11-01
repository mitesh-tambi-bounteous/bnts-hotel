package com.bounteous.hotel.bookingworker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatusControllerTest {
    @Test
    void reportsUp() {
        assertEquals("UP", new StatusController().status().get("status"));
    }
}
