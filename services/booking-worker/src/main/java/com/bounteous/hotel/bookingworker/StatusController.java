package com.bounteous.hotel.bookingworker;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("service", "booking-worker", "status", "UP");
    }
}
