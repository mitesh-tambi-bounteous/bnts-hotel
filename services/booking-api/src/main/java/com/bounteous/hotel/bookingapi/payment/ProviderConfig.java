package com.bounteous.hotel.bookingapi.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Picks the payment gateway from the environment. Blank Razorpay keys fail startup unless
 * {@code payments.in-memory=true} is set explicitly (local profile / tests); that is never
 * honoured in the qa and prod profiles.
 */
@Configuration
public class ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    @Bean
    public PaymentGateway paymentGateway(Environment env) {
        String keyId = env.getProperty("razorpay.key-id", "");
        String keySecret = env.getProperty("razorpay.key-secret", "");
        if (!keyId.isBlank() && !keySecret.isBlank()) {
            return new RazorpayPaymentGateway(new SdkRazorpayApi(keyId, keySecret), keyId, keySecret);
        }
        // Fail closed: the gateway that accepts any payment id must be asked for explicitly.
        if (env.acceptsProfiles(Profiles.of("qa", "prod")) || !env.getProperty("payments.in-memory", Boolean.class, false)) {
            throw new IllegalStateException(
                    "RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET must be set (payments.in-memory=true is for local use only)");
        }
        log.warn("Razorpay keys are blank: using the in-memory payment gateway. No card is charged.");
        return new InMemoryPaymentGateway();
    }
}
