package com.bounteous.hotel.bookingapi.payment;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;

/** {@link RazorpayApi} over the official SDK. Not unit-tested: it only maps calls to the SDK. */
public class SdkRazorpayApi implements RazorpayApi {

    private final RazorpayClient client;

    public SdkRazorpayApi(String keyId, String keySecret) {
        try {
            this.client = new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Could not create the Razorpay client", e);
        }
    }

    @Override
    public String createOrder(long amountPaise, String receipt) {
        JSONObject request = new JSONObject()
                .put("amount", amountPaise)
                .put("currency", "INR")
                .put("receipt", receipt)
                .put("payment_capture", true);
        try {
            return client.orders.create(request).get("id");
        } catch (RazorpayException e) {
            throw new IllegalStateException("Razorpay order could not be created", e);
        }
    }

    @Override
    public RazorpayPayment fetchPayment(String paymentId) {
        try {
            var payment = client.payments.fetch(paymentId);
            String status = payment.get("status");
            String orderId = payment.get("order_id");
            Number amount = payment.get("amount");
            return new RazorpayPayment(status, orderId, amount.longValue());
        } catch (RazorpayException e) {
            throw new PaymentVerificationException("Payment could not be verified with the provider");
        }
    }

    @Override
    public void refund(String paymentId) {
        JSONObject request = new JSONObject().put("receipt", "refund_" + paymentId);
        try {
            client.payments.refund(paymentId, request);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Razorpay refund failed for " + paymentId, e);
        }
    }
}
