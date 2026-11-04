package com.bounteous.hotel.bookingworker.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EmailConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(EmailConfig.class);

    @ParameterizedTest
    @ValueSource(strings = {"qa", "prod"})
    void aBlankSenderStopsStartupInQaAndProd(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile, "email.from-address=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void aBlankSenderLogsEmailsElsewhere() {
        runner.withPropertyValues("email.from-address=")
                .run(ctx -> assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LoggingEmailSender.class));
    }

    @Test
    void aSenderAddressSelectsSes() {
        runner.withPropertyValues("email.from-address=bookings@example.com", "email.region=ap-south-1")
                .withSystemProperties("aws.accessKeyId=fake-key", "aws.secretAccessKey=fake-secret")
                .run(ctx -> assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(SesEmailSender.class));
    }
}
