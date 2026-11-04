package com.bounteous.hotel.bookingapi.payment;

/** Thin seam over the Razorpay SDK so the gateway can be tested without the network. */
public interface RazorpayApi {

    /** Creates an auto-capture INR order in paise and returns its id. */
    String createOrder(long amountPaise, String receipt);

    RazorpayPayment fetchPayment(String paymentId);

    /** Full refund; idempotent per payment. */
    void refund(String paymentId);

    record RazorpayPayment(String status, String orderId, long amountPaise) {}
}
