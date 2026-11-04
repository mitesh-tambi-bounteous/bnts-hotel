package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.payment.PaymentProof;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    /** Body of POST /bookings: the stay, the guest and Razorpay's proof of payment (no card data). */
    public record BookingRequest(
            Long hotelId,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            List<Long> roomTypeIds,
            String name,
            String email,
            String phone,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {}

    private final BookingService service;
    private final BookingRepository bookings;

    public BookingController(BookingService service, BookingRepository bookings) {
        this.service = service;
        this.bookings = bookings;
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResult book(@RequestBody BookingRequest request) {
        if (request.hotelId() == null) {
            throw new NotFoundException("Hotel not found");
        }
        return service.book(new CheckoutCommand(
                request.hotelId(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.roomTypeIds(),
                request.name(),
                request.email(),
                request.phone(),
                new PaymentProof(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())));
    }

    @GetMapping("/bookings/{reference}")
    public BookingView get(@PathVariable String reference) {
        return bookings.findByReference(reference).orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
