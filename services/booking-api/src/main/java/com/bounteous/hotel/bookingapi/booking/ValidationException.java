package com.bounteous.hotel.bookingapi.booking;

import java.util.Map;

/** The request is missing or has invalid fields; carries one message per field. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("Invalid request: " + fieldErrors.keySet());
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
