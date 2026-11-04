package com.bounteous.hotel.bookingapi.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bounteous.hotel.bookingapi.booking.BookingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentReconcilerTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final PaymentReconciler reconciler = new PaymentReconciler(bookings, gateway, 15);
    private final Instant now = Instant.parse("2030-01-01T10:00:00Z");

    @Test
    void refundsCapturedPaymentsWithNoBookingOlderThanTheGracePeriod() {
        when(bookings.findOrphanedPayments(now.minus(Duration.ofMinutes(15)))).thenReturn(List.of("pay_orphan"));

        assertThat(reconciler.reconcile(now)).isEqualTo(1);

        verify(gateway).refund("pay_orphan");
        verify(bookings).markPaymentRefunded("pay_orphan");
    }

    @Test
    void doesNothingWhenEveryPaymentHasABooking() {
        when(bookings.findOrphanedPayments(any())).thenReturn(List.of());

        assertThat(reconciler.reconcile(now)).isZero();

        verify(gateway, never()).refund(any());
    }

    @Test
    void aFailedRefundIsNotMarkedRefundedAndDoesNotStopTheRest() {
        when(bookings.findOrphanedPayments(any())).thenReturn(List.of("pay_bad", "pay_ok"));
        doThrow(new IllegalStateException("provider down")).when(gateway).refund("pay_bad");

        assertThat(reconciler.reconcile(now)).isEqualTo(1);

        verify(bookings, never()).markPaymentRefunded("pay_bad");
        verify(bookings).markPaymentRefunded("pay_ok");
    }
}
