package com.bounteous.hotel.bookingapi.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RazorpaySignatureTest {

    static final String SECRET = "test-secret";

    static String sign(String payload) {
        return RazorpaySignature.compute(payload, SECRET);
    }

    @Test
    void acceptsASignatureComputedWithTheKeySecret() {
        assertThat(RazorpaySignature.verify("order_1", "pay_1", sign("order_1|pay_1"), SECRET)).isTrue();
    }

    @Test
    void rejectsATamperedPaymentIdOrSignature() {
        assertThat(RazorpaySignature.verify("order_1", "pay_2", sign("order_1|pay_1"), SECRET)).isFalse();
        assertThat(RazorpaySignature.verify("order_1", "pay_1", "deadbeef", SECRET)).isFalse();
        assertThat(RazorpaySignature.verify("order_1", "pay_1", null, SECRET)).isFalse();
    }

    @Test
    void rejectsASignatureMadeWithAnotherSecret() {
        var forged = RazorpaySignature.compute("order_1|pay_1", "other-secret");
        assertThat(RazorpaySignature.verify("order_1", "pay_1", forged, SECRET)).isFalse();
    }
}
