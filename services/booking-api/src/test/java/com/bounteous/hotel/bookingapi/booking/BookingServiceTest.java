package com.bounteous.hotel.bookingapi.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bounteous.hotel.bookingapi.account.GuestAccountRepository;
import com.bounteous.hotel.bookingapi.account.GuestAccountRepository.AccountRef;
import com.bounteous.hotel.bookingapi.email.ConfirmationEmailPayload;
import com.bounteous.hotel.bookingapi.email.EmailOutbox;
import com.bounteous.hotel.bookingapi.payment.PaymentGateway;
import com.bounteous.hotel.bookingapi.payment.PaymentProof;
import com.bounteous.hotel.bookingapi.payment.PaymentVerificationException;
import com.bounteous.hotel.bookingapi.search.FakeCatalogue;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

class BookingServiceTest {

    private static final LocalDate IN = LocalDate.of(2026, 12, 12);
    private static final LocalDate OUT = LocalDate.of(2026, 12, 14);
    private static final PaymentProof PROOF = new PaymentProof("order_1", "pay_1", "sig");

    private final PaymentGateway payments = mock(PaymentGateway.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final GuestAccountRepository accounts = mock(GuestAccountRepository.class);
    private final EmailOutbox outbox = mock(EmailOutbox.class);
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(
                new StayPricer(new FakeCatalogue()),
                payments,
                bookings,
                accounts,
                outbox,
                TransactionOperations.withoutTransaction());
        when(payments.charge(anyLong(), any())).thenReturn("pay_1");
        when(bookings.recordPayment(any(), anyLong())).thenReturn(7L);
        when(bookings.reserve(anyLong(), any(), any())).thenReturn(true);
        when(accounts.findOrCreate(any())).thenReturn(new AccountRef(5L, true));
        when(bookings.save(any())).thenReturn("HB-7K2M9Q");
    }

    private CheckoutCommand cmd(String name, String email, String phone, Long... roomTypeIds) {
        return new CheckoutCommand(
                FakeCatalogue.HARBOUR, IN, OUT, 4, List.of(roomTypeIds), name, email, phone, PROOF);
    }

    private CheckoutCommand cmd(String name, String email, String phone) {
        return cmd(name, email, phone, FakeCatalogue.DELUXE, FakeCatalogue.EXECUTIVE);
    }

    @Test
    void aBlankNameMakesNoPaymentAndNoBooking() { // AC6
        assertThatThrownBy(() -> service.book(cmd("", "a@b.com", null)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).fieldErrors())
                        .containsEntry("name", "Enter your full name"));

        verify(payments, never()).charge(anyLong(), any());
        verify(bookings, never()).save(any());
    }

    @Test
    void aBlankEmailMakesNoPaymentAndNoBooking() { // AC7
        assertThatThrownBy(() -> service.book(cmd("Asha Rao", " ", null)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).fieldErrors())
                        .containsEntry("email", "Enter your email address"));

        verify(payments, never()).charge(anyLong(), any());
        verify(bookings, never()).save(any());
    }

    @Test
    void completesWithoutAPhoneNumber() { // AC8
        var result = service.book(cmd("Asha Rao", "asha@example.com", null));

        assertThat(result.reference()).isEqualTo("HB-7K2M9Q");
        var booking = ArgumentCaptor.forClass(NewBooking.class);
        verify(bookings).save(booking.capture());
        assertThat(booking.getValue().guestPhone()).isNull();
    }

    @Test
    void takesOneCardPaymentForTheWholeBooking() { // AC9
        service.book(cmd("Asha Rao", "asha@example.com", null));

        // Deluxe 7,800 + Executive 9,600 for 2 nights, in paise.
        verify(payments, times(1)).charge(eq(2 * (780_000L + 960_000L)), eq(PROOF));
    }

    @Test
    void putsBothRoomsOnOneBooking() { // AC10
        service.book(cmd("Asha Rao", "asha@example.com", null));

        var booking = ArgumentCaptor.forClass(NewBooking.class);
        verify(bookings, times(1)).save(booking.capture());
        assertThat(booking.getValue().rooms()).hasSize(2);
        assertThat(booking.getValue().totalPaise()).isEqualTo(3_480_000L);
    }

    @Test
    void queuesOneConfirmationEmailForTheCheckoutEmailInsteadOfSendingIt() { // AC12
        service.book(cmd("Asha Rao", "Asha@Example.com", null));

        var payload = ArgumentCaptor.forClass(ConfirmationEmailPayload.class);
        verify(outbox, times(1)).enqueue(eq("asha@example.com"), payload.capture());
        assertThat(payload.getValue().reference()).isEqualTo("HB-7K2M9Q");
        assertThat(payload.getValue().firstName()).isEqualTo("Asha");
        assertThat(payload.getValue().rooms()).extracting(ConfirmationEmailPayload.RoomLine::name)
                .containsExactly("Deluxe sea-facing room", "Executive room");
        assertThat(payload.getValue().totalPaise()).isEqualTo(3_480_000L);
    }

    @Test
    void looksUpTheAccountByLowerCasedEmail() { // AC14, AC15
        service.book(cmd("Priya", "Priya@Example.com", null));

        verify(accounts).findOrCreate("priya@example.com");
    }

    @Test
    void reversesThePaymentWhenTheBookingCannotBeSaved() { // AC18
        when(bookings.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOf(BookingFailedException.class);

        verify(payments).refund("pay_1");
        verify(bookings).markPaymentRefunded("pay_1");
    }

    @Test
    void reversesThePaymentWhenQueuingTheEmailFails() {
        org.mockito.Mockito.doThrow(new IllegalStateException("outbox down")).when(outbox).enqueue(any(), any());

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOf(BookingFailedException.class);

        verify(payments).refund("pay_1");
    }

    @Test
    void refundsAndReportsTheRoomWhenAnotherGuestTookItAfterTheCharge() { // AC16
        when(bookings.reserve(eq(FakeCatalogue.EXECUTIVE), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOfSatisfying(
                        RoomUnavailableException.class,
                        e -> assertThat(e.roomTypeId()).isEqualTo(FakeCatalogue.EXECUTIVE));

        verify(payments).refund("pay_1");
        verify(bookings, never()).save(any());
    }

    @Test
    void doesNotChargeWhenARoomIsAlreadySoldOut() { // AC16
        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null, FakeCatalogue.SUITE)))
                .isInstanceOf(RoomUnavailableException.class);

        verify(payments, never()).charge(anyLong(), any());
    }

    @Test
    void doesNotBookWhenThePaymentCannotBeVerified() {
        when(payments.charge(anyLong(), any())).thenThrow(new PaymentVerificationException("bad signature"));

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOf(PaymentVerificationException.class);

        verify(bookings, never()).save(any());
        verify(payments, never()).refund(any());
    }

    @Test
    void neverRefundsAPaymentThatAlreadyPaidForAnotherBooking() {
        when(bookings.recordPayment(any(), anyLong()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOf(PaymentVerificationException.class);

        verify(payments, never()).refund(any());
    }

    @Test
    void rejectsAnUnsuitableRoomBeforeAnyCharge() { // AC21, AC22
        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null, FakeCatalogue.COMPACT, FakeCatalogue.DELUXE)))
                .isInstanceOf(ValidationException.class);

        verify(payments, never()).charge(anyLong(), any());
    }

    @Test
    void aDoubleSubmitOfASuccessfulBookingReturnsThatBooking() {
        when(bookings.findPayment("pay_1"))
                .thenReturn(java.util.Optional.of(
                        new BookingRepository.PaymentRecord("CAPTURED", "HB-7K2M9Q", true, "asha@example.com")));

        BookingResult result = service.book(cmd("Asha Rao", "Asha@Example.com", null));

        assertThat(result.reference()).isEqualTo("HB-7K2M9Q");
        verify(payments, never()).charge(anyLong(), any());
        verify(bookings, never()).save(any());
    }

    @Test
    void aReversedPaymentIsReportedAsReversed() {
        when(bookings.findPayment("pay_1"))
                .thenReturn(java.util.Optional.of(new BookingRepository.PaymentRecord("REFUNDED", null, false, null)));

        assertThatThrownBy(() -> service.book(cmd("Asha Rao", "asha@example.com", null)))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("reversed");

        verify(payments, never()).charge(anyLong(), any());
    }
}
