package com.bounteous.hotel.bookingapi.payment;

/** Port over the card payment provider. */
public interface PaymentGateway {

    /** Creates a provider order for the server-computed total (paise, INR). */
    PaymentOrder createOrder(long amountPaise, String receipt);

    /**
     * Verifies the guest's payment covers exactly {@code totalPaise} and returns the provider's
     * payment id. Called once per booking.
     */
    String charge(long totalPaise, PaymentProof proof);

    /** Reverses a payment in full. */
    void refund(String paymentId);
}
