package com.bounteous.hotel.bookingapi.booking;

import com.bounteous.hotel.bookingapi.account.GuestAccountRepository;
import com.bounteous.hotel.bookingapi.account.GuestAccountRepository.AccountRef;
import com.bounteous.hotel.bookingapi.booking.BookingRepository.PaymentRecord;
import com.bounteous.hotel.bookingapi.booking.StayPricer.Quote;
import com.bounteous.hotel.bookingapi.catalogue.RoomType;
import com.bounteous.hotel.bookingapi.email.ConfirmationEmailPayload;
import com.bounteous.hotel.bookingapi.email.EmailOutbox;
import com.bounteous.hotel.bookingapi.payment.PaymentGateway;
import com.bounteous.hotel.bookingapi.payment.PaymentVerificationException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Books the selected rooms. Order matters: validate, check availability, take ONE payment for the
 * whole booking, then in one transaction reserve inventory (conditional decrement), find or create
 * the account, save the booking with all its rooms and queue the confirmation email. Any failure
 * after the charge reverses the payment.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final StayPricer pricer;
    private final PaymentGateway payments;
    private final BookingRepository bookings;
    private final GuestAccountRepository accounts;
    private final EmailOutbox outbox;
    private final TransactionOperations transaction;

    public BookingService(
            StayPricer pricer,
            PaymentGateway payments,
            BookingRepository bookings,
            GuestAccountRepository accounts,
            EmailOutbox outbox,
            TransactionOperations transaction) {
        this.pricer = pricer;
        this.payments = payments;
        this.bookings = bookings;
        this.accounts = accounts;
        this.outbox = outbox;
        this.transaction = transaction;
    }

    public BookingResult book(CheckoutCommand cmd) {
        validate(cmd);
        BookingResult replay = replayOf(cmd);
        if (replay != null) {
            return replay;
        }
        Quote quote = pricer.price(cmd.hotelId(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.roomTypeIds());

        String paymentId = payments.charge(quote.totalPaise(), cmd.proof());
        long paymentRowId;
        try {
            paymentRowId = bookings.recordPayment(paymentId, quote.totalPaise());
        } catch (DuplicateKeyException e) {
            // This payment already pays for another booking: never refund it.
            throw new PaymentVerificationException("This payment has already been used");
        } catch (RuntimeException e) {
            throw failAfterCharge(paymentId, e);
        }

        try {
            return transaction.execute(status -> save(cmd, quote, paymentRowId));
        } catch (RoomUnavailableException e) {
            reverse(paymentId);
            throw e;
        } catch (RuntimeException e) {
            throw failAfterCharge(paymentId, e);
        }
    }

    /**
     * A payment id we have already seen: a double-submit of a booking that succeeded returns that
     * booking (only to the same email), a reversed payment says so, and anything else is refused.
     */
    private BookingResult replayOf(CheckoutCommand cmd) {
        if (cmd.proof() == null || cmd.proof().paymentId() == null) {
            return null;
        }
        PaymentRecord prior = bookings.findPayment(cmd.proof().paymentId()).orElse(null);
        if (prior == null) {
            return null;
        }
        if (prior.bookingReference() != null && normaliseEmail(cmd.email()).equals(prior.email())) {
            return new BookingResult(prior.bookingReference(), prior.accountCreated());
        }
        if ("REFUNDED".equals(prior.status())) {
            throw new PaymentVerificationException("This payment was reversed. Please choose your rooms and pay again");
        }
        throw new PaymentVerificationException("This payment has already been used");
    }

    private BookingResult save(CheckoutCommand cmd, Quote quote, long paymentRowId) {
        // Reserve in a fixed order so concurrent bookings cannot deadlock on each other.
        List<RoomType> inLockOrder = quote.rooms().stream().sorted(Comparator.comparingLong(RoomType::id)).toList();
        for (RoomType room : inLockOrder) {
            if (!bookings.reserve(room.id(), cmd.checkIn(), cmd.checkOut())) {
                throw new RoomUnavailableException(room.id());
            }
        }
        String email = normaliseEmail(cmd.email());
        String name = cmd.name().trim();
        AccountRef account = accounts.findOrCreate(email);
        String reference = bookings.save(new NewBooking(
                account.id(),
                account.created(),
                cmd.hotelId(),
                cmd.checkIn(),
                cmd.checkOut(),
                cmd.guests(),
                quote.totalPaise(),
                paymentRowId,
                name,
                blankToNull(cmd.phone()),
                quote.rooms()));
        outbox.enqueue(email, confirmationEmail(name, reference, cmd, quote));
        return new BookingResult(reference, account.created());
    }

    private static ConfirmationEmailPayload confirmationEmail(
            String name, String reference, CheckoutCommand cmd, Quote quote) {
        List<ConfirmationEmailPayload.RoomLine> lines = quote.rooms().stream()
                .map(room -> new ConfirmationEmailPayload.RoomLine(room.name(), room.pricePerNightPaise() * quote.nights()))
                .toList();
        return new ConfirmationEmailPayload(
                name.split("\\s+")[0],
                reference,
                quote.hotel().name(),
                cmd.checkIn().toString(),
                cmd.checkOut().toString(),
                quote.nights(),
                lines,
                quote.totalPaise());
    }

    private BookingFailedException failAfterCharge(String paymentId, RuntimeException cause) {
        log.error("Booking could not be saved after the card was charged; reversing payment {}", paymentId, cause);
        reverse(paymentId);
        return new BookingFailedException("The booking could not be saved", cause);
    }

    private void reverse(String paymentId) {
        try {
            payments.refund(paymentId);
            bookings.markPaymentRefunded(paymentId);
        } catch (RuntimeException e) {
            log.error("MANUAL REFUND NEEDED: payment {} was charged but could not be reversed", paymentId, e);
        }
    }

    private static void validate(CheckoutCommand cmd) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (cmd.name() == null || cmd.name().isBlank()) {
            errors.put("name", "Enter your full name");
        }
        if (cmd.email() == null || cmd.email().isBlank()) {
            errors.put("email", "Enter your email address");
        } else if (!cmd.email().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            errors.put("email", "Enter a valid email address");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
