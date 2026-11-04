package com.bounteous.hotel.bookingapi.payment;

import com.bounteous.hotel.bookingapi.payment.RazorpayApi.RazorpayPayment;

public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayApi api;
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentGateway(RazorpayApi api, String keyId, String keySecret) {
        this.api = api;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public PaymentOrder createOrder(long amountPaise, String receipt) {
        return new PaymentOrder(api.createOrder(amountPaise, receipt), amountPaise, keyId);
    }

    @Override
    public String charge(long totalPaise, PaymentProof proof) {
        if (proof == null
                || !RazorpaySignature.verify(proof.orderId(), proof.paymentId(), proof.signature(), keySecret)) {
            throw new PaymentVerificationException("Payment signature is not valid");
        }
        RazorpayPayment payment = api.fetchPayment(proof.paymentId());
        if (!"captured".equals(payment.status())) {
            throw new PaymentVerificationException("Payment was not captured");
        }
        if (!proof.orderId().equals(payment.orderId())) {
            throw new PaymentVerificationException("Payment belongs to a different order");
        }
        if (payment.amountPaise() != totalPaise) {
            throw new PaymentVerificationException("Payment amount does not match the booking total");
        }
        return proof.paymentId();
    }

    @Override
    public void refund(String paymentId) {
        api.refund(paymentId);
    }
}
