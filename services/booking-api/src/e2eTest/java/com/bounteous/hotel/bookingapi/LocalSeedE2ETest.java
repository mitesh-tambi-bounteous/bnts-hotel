package com.bounteous.hotel.bookingapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The dev seed is on the Flyway locations only under the local profile and is safe to run twice. */
@Testcontainers
@SpringBootTest
@ActiveProfiles("local")
class LocalSeedE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test
    void v1AppliesAndTheLocalSeedLoadsOnceAndIsIdempotent() {
        int hotels = jdbc.queryForObject("SELECT count(*) FROM hotel", Integer.class);
        int inventory = jdbc.queryForObject("SELECT count(*) FROM room_inventory", Integer.class);
        assertThat(hotels).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_account WHERE email = 'priya@example.com'", Integer.class))
                .isEqualTo(1);

        new ResourceDatabasePopulator(new FileSystemResource(Path.of("src/main/resources/db/dev/R__dev_seed.sql")))
                .execute(dataSource);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM hotel", Integer.class)).isEqualTo(hotels);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_inventory", Integer.class)).isEqualTo(inventory);
    }
}
