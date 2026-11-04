package com.bounteous.hotel.bookingapi.payment;

/** A payment order created on the server; keyId is empty when no real provider is configured. */
public record PaymentOrder(String orderId, long amountPaise, String keyId) {}
