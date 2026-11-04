package com.bounteous.hotel.bookingapi.payment;

import com.bounteous.hotel.bookingapi.booking.BookingRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for a crash between capturing a payment and saving its booking: refunds any CAPTURED
 * payment that still has no booking after a grace period (long enough for an in-flight booking).
 */
@Component
public class PaymentReconciler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciler.class);

    private final BookingRepository bookings;
    private final PaymentGateway payments;
    private final Duration grace;

    public PaymentReconciler(
            BookingRepository bookings,
            PaymentGateway payments,
            @Value("${payments.reconcile-grace-minutes:15}") long graceMinutes) {
        this.bookings = bookings;
        this.payments = payments;
        this.grace = Duration.ofMinutes(graceMinutes);
    }

    @Scheduled(
            fixedDelayString = "${payments.reconcile-interval-ms:300000}",
            initialDelayString = "${payments.reconcile-interval-ms:300000}")
    public void reconcile() {
        reconcile(Instant.now());
    }

    /** Returns how many orphaned payments were refunded. */
    public int reconcile(Instant now) {
        int refunded = 0;
        for (String providerRef : bookings.findOrphanedPayments(now.minus(grace))) {
            try {
                payments.refund(providerRef);
                bookings.markPaymentRefunded(providerRef);
                refunded++;
                log.warn("Refunded payment {}: it was captured but no booking was saved", providerRef);
            } catch (RuntimeException e) {
                log.error("MANUAL REFUND NEEDED: orphaned payment {} could not be refunded", providerRef, e);
            }
        }
        return refunded;
    }
}
