package com.bounteous.hotel.bookingapi.payment;

import static com.bounteous.hotel.bookingapi.payment.RazorpaySignatureTest.SECRET;
import static com.bounteous.hotel.bookingapi.payment.RazorpaySignatureTest.sign;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bounteous.hotel.bookingapi.payment.RazorpayApi.RazorpayPayment;
import org.junit.jupiter.api.Test;

class RazorpayPaymentGatewayTest {

    private final RazorpayApi api = mock(RazorpayApi.class);
    private final RazorpayPaymentGateway gateway = new RazorpayPaymentGateway(api, "key_test", SECRET);
    private final PaymentProof proof = new PaymentProof("order_1", "pay_1", sign("order_1|pay_1"));

    @Test
    void acceptsACapturedPaymentForTheExactAmount() {
        when(api.fetchPayment("pay_1")).thenReturn(new RazorpayPayment("captured", "order_1", 1_000L));

        assertThat(gateway.charge(1_000L, proof)).isEqualTo("pay_1");
    }

    @Test
    void rejectsAWrongAmountBeforeAnythingIsBooked() {
        when(api.fetchPayment("pay_1")).thenReturn(new RazorpayPayment("captured", "order_1", 1_000L));

        assertThatThrownBy(() -> gateway.charge(2_000L, proof)).isInstanceOf(PaymentVerificationException.class);
    }

    @Test
    void rejectsATamperedSignatureWithoutAskingTheProvider() {
        var tampered = new PaymentProof("order_1", "pay_1", "deadbeef");

        assertThatThrownBy(() -> gateway.charge(1_000L, tampered)).isInstanceOf(PaymentVerificationException.class);
        verify(api, never()).fetchPayment(any());
    }

    @Test
    void rejectsAPaymentThatWasNotCapturedOrBelongsToAnotherOrder() {
        when(api.fetchPayment("pay_1")).thenReturn(new RazorpayPayment("authorized", "order_1", 1_000L));
        assertThatThrownBy(() -> gateway.charge(1_000L, proof)).isInstanceOf(PaymentVerificationException.class);

        when(api.fetchPayment("pay_1")).thenReturn(new RazorpayPayment("captured", "order_9", 1_000L));
        assertThatThrownBy(() -> gateway.charge(1_000L, proof)).isInstanceOf(PaymentVerificationException.class);
    }

    @Test
    void createsAnOrderForTheServerTotalAndReturnsTheKeyId() {
        when(api.createOrder(anyLong(), any())).thenReturn("order_9");

        var order = gateway.createOrder(3_120_000L, "bk_1");

        assertThat(order).isEqualTo(new PaymentOrder("order_9", 3_120_000L, "key_test"));
    }

    @Test
    void refundsThroughTheProvider() {
        gateway.refund("pay_1");

        verify(api).refund("pay_1");
    }
}
