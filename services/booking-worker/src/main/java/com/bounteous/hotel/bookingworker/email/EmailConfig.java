package com.bounteous.hotel.bookingworker.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class EmailConfig {
    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    EmailSender emailSender(
            Environment env,
            @Value("${email.from-address:}") String fromAddress,
            @Value("${email.region:ap-south-1}") String region) {
        if (fromAddress == null || fromAddress.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("qa", "prod"))) {
                throw new IllegalStateException("SES_FROM_ADDRESS must be set in qa and prod");
            }
            log.warn("SES_FROM_ADDRESS is blank: confirmation emails are logged, not sent");
            return new LoggingEmailSender();
        }
        return new SesEmailSender(SesV2Client.builder().region(Region.of(region)).build(), fromAddress);
    }

    @Bean
    ConfirmationEmailTemplate confirmationEmailTemplate() {
        return new ConfirmationEmailTemplate();
    }
}
