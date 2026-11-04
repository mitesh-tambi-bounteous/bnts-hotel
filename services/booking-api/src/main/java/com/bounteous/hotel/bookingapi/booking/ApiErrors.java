package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.payment.PaymentVerificationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps domain exceptions to the API's error bodies: {code, message, fieldErrors?, roomTypeId?}. */
@RestControllerAdvice
public class ApiErrors {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, Map<String, String> fieldErrors, Long roomTypeId) {}

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorBody> validation(ValidationException e) {
        return respond(HttpStatus.BAD_REQUEST, new ErrorBody("VALIDATION", e.getMessage(), e.fieldErrors(), null));
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ErrorBody> badRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, new ErrorBody("BAD_REQUEST", "The request is not valid", null, null));
    }

    @ExceptionHandler(RoomUnavailableException.class)
    ResponseEntity<ErrorBody> roomUnavailable(RoomUnavailableException e) {
        return respond(
                HttpStatus.CONFLICT,
                new ErrorBody("ROOM_UNAVAILABLE", "That room is no longer available", null, e.roomTypeId()));
    }

    @ExceptionHandler(BookingFailedException.class)
    ResponseEntity<ErrorBody> bookingFailed(BookingFailedException e) {
        return respond(
                HttpStatus.BAD_GATEWAY,
                new ErrorBody("BOOKING_FAILED", "The booking could not be completed and the payment was reversed", null, null));
    }

    @ExceptionHandler(PaymentVerificationException.class)
    ResponseEntity<ErrorBody> paymentNotVerified(PaymentVerificationException e) {
        return respond(HttpStatus.PAYMENT_REQUIRED, new ErrorBody("PAYMENT_FAILED", e.getMessage(), null, null));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorBody> notFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, new ErrorBody("NOT_FOUND", e.getMessage(), null, null));
    }

    private static ResponseEntity<ErrorBody> respond(HttpStatus status, ErrorBody body) {
        return ResponseEntity.status(status).body(body);
    }
}
