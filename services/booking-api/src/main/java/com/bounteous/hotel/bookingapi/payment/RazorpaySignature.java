package com.bounteous.hotel.bookingapi.payment;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Razorpay's checkout signature: HMAC_SHA256(orderId + "|" + paymentId, keySecret), hex. */
public final class RazorpaySignature {

    private RazorpaySignature() {}

    public static boolean verify(String orderId, String paymentId, String signature, String keySecret) {
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }
        byte[] expected = compute(orderId + "|" + paymentId, keySecret).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    static String compute(String payload, String keySecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }
}
