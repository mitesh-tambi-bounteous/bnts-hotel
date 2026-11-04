package com.bounteous.hotel.bookingapi.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProviderConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ProviderConfig.class);

    @ParameterizedTest
    @ValueSource(strings = {"qa", "prod"})
    void blankKeysStopStartupInQaAndProd(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile, "razorpay.key-id=", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void oneBlankKeyIsStillBlank() {
        runner.withPropertyValues("spring.profiles.active=prod", "razorpay.key-id=key_test", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void blankKeysStopStartupWithNoProfileOrAnUnknownOne() {
        runner.withPropertyValues("razorpay.key-id=", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasFailed());
        runner.withPropertyValues("spring.profiles.active=prd", "razorpay.key-id=", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void theInMemoryGatewayNeedsAnExplicitOptIn() {
        runner.withPropertyValues("payments.in-memory=true", "razorpay.key-id=", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasSingleBean(InMemoryPaymentGateway.class));
    }

    @Test
    void theOptInIsIgnoredInProd() {
        runner.withPropertyValues(
                        "spring.profiles.active=prod", "payments.in-memory=true", "razorpay.key-id=", "razorpay.key-secret=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void bothKeysSelectTheRazorpayGateway() {
        runner.withPropertyValues("razorpay.key-id=key_test", "razorpay.key-secret=test-secret")
                .run(ctx -> assertThat(ctx).hasSingleBean(RazorpayPaymentGateway.class));
    }
}
