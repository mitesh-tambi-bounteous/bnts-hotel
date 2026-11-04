package com.bounteous.hotel.bookingapi.payment;

/** What the browser sends back after paying: the provider's order, payment id and signature. Never card data. */
public record PaymentProof(String orderId, String paymentId, String signature) {}
