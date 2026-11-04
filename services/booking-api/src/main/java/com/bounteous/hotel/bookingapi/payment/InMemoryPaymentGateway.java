package com.bounteous.hotel.bookingapi.payment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Takes no real payments: used by tests and by local/dev when no Razorpay keys are set. */
public class InMemoryPaymentGateway implements PaymentGateway {

    private final AtomicLong orders = new AtomicLong();
    private final List<String> charged = new CopyOnWriteArrayList<>();
    private final List<String> refunded = new CopyOnWriteArrayList<>();

    @Override
    public PaymentOrder createOrder(long amountPaise, String receipt) {
        return new PaymentOrder("order_fake_" + orders.incrementAndGet(), amountPaise, "");
    }

    @Override
    public String charge(long totalPaise, PaymentProof proof) {
        if (proof == null || proof.paymentId() == null || proof.paymentId().isBlank()) {
            throw new PaymentVerificationException("Missing payment proof");
        }
        charged.add(proof.paymentId());
        return proof.paymentId();
    }

    @Override
    public void refund(String paymentId) {
        refunded.add(paymentId);
    }

    public List<String> charged() {
        return List.copyOf(charged);
    }

    public List<String> refunded() {
        return List.copyOf(refunded);
    }
}
