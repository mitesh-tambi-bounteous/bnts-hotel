package com.bounteous.hotel.bookingapi.payment;

import com.bounteous.hotel.bookingapi.booking.StayPricer;
import com.bounteous.hotel.bookingapi.booking.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentOrderController {

    /** Body of POST /payments/orders: what the guest selected. The amount is never sent by the client. */
    public record OrderRequest(
            Long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, List<Long> roomTypeIds) {}

    private final StayPricer pricer;
    private final PaymentGateway gateway;

    public PaymentOrderController(StayPricer pricer, PaymentGateway gateway) {
        this.pricer = pricer;
        this.gateway = gateway;
    }

    @PostMapping("/payments/orders")
    public PaymentOrder createOrder(@RequestBody OrderRequest request) {
        if (request.hotelId() == null) {
            throw new NotFoundException("Hotel not found");
        }
        var quote = pricer.price(
                request.hotelId(), request.checkIn(), request.checkOut(), request.guests(), request.roomTypeIds());
        return gateway.createOrder(quote.totalPaise(), "bk_" + UUID.randomUUID());
    }
}
