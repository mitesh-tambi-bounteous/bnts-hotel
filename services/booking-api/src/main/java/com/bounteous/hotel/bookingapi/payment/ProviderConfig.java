package com.bounteous.hotel.bookingapi.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Picks the payment gateway from the environment. Blank Razorpay keys fall back to the in-memory
 * gateway (local/dev) except in the qa and prod profiles, where startup fails.
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
        if (env.acceptsProfiles(Profiles.of("qa", "prod"))) {
            throw new IllegalStateException("RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET must be set in qa and prod");
        }
        log.warn("Razorpay keys are blank: using the in-memory payment gateway. No card is charged.");
        return new InMemoryPaymentGateway();
    }
}
