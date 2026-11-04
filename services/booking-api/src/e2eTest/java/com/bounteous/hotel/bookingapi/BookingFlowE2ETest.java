package com.bounteous.hotel.bookingapi;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.bounteous.hotel.bookingapi.email.EmailOutbox;
import com.bounteous.hotel.bookingapi.payment.PaymentGateway;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Walks search, detail, order and checkout against PostgreSQL loaded by ops/load_catalogue.sql. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingFlowE2ETest {

    private static final Path LOADER = Path.of("ops/load_catalogue.sql");
    private static final long HARBOUR = 1;
    private static final long DELUXE = 101;
    private static final long EXECUTIVE = 102;
    private static final long SUITE = 103;
    private static final long MARINE = 4;
    private static final long MARINE_ROOM = 401;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PaymentGateway gateway;
    @MockitoSpyBean EmailOutbox outbox;

    private final LocalDate checkIn = LocalDate.now().plusDays(10);
    private final LocalDate checkOut = checkIn.plusDays(2);

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        runLoader();
    }

    private void runLoader() {
        new ResourceDatabasePopulator(new FileSystemResource(LOADER)).execute(dataSource);
    }

    private Map<String, Object> stay(long hotelId, int guests, Long... rooms) {
        return Map.of(
                "hotelId", hotelId, "checkIn", checkIn.toString(), "checkOut", checkOut.toString(),
                "guests", guests, "roomTypeIds", List.of(rooms));
    }

    private Map<String, Object> checkout(String name, String email, String paymentId, long hotelId, Long... rooms) {
        var body = new java.util.HashMap<>(stay(hotelId, 4, rooms));
        body.put("name", name);
        body.put("email", email);
        body.put("razorpayOrderId", "order_fake_1");
        body.put("razorpayPaymentId", paymentId);
        body.put("razorpaySignature", "fake");
        return body;
    }

    private io.restassured.response.Response book(Map<String, Object> body) {
        return given().contentType(ContentType.JSON).body(body).post("/bookings");
    }

    @Test
    void searchListsTheBrandsHotelsWithPricesAndSoldOutAndUnsuitableRooms() { // AC1-4, 20-23
        given().queryParam("city", "Mumbai").queryParam("checkIn", checkIn).queryParam("checkOut", checkOut)
                .queryParam("guests", 4).queryParam("rooms", 2)
                .get("/hotels").then().statusCode(200)
                .body("name", hasItem("Bandra Court"))
                .body("name", hasItem("Harbour View Mumbai"))
                .body("find { it.name == 'Harbour View Mumbai' }.fromPricePerNightPaise", is(780_000));

        given().queryParam("checkIn", checkIn).queryParam("checkOut", checkOut)
                .queryParam("guests", 4).queryParam("rooms", 2)
                .get("/hotels/{id}", HARBOUR).then().statusCode(200)
                .body("tabs.name", is(List.of("Overview", "Amenities", "Location")))
                .body("rooms.find { it.name == 'Harbour suite' }.status", is("SOLD_OUT"))
                .body("rooms.find { it.name == 'Compact single room' }.status", is("UNSUITABLE"))
                .body("rooms.find { it.name == 'Deluxe sea-facing room' }.status", is("AVAILABLE"));

        given().queryParam("city", "Goa").queryParam("checkIn", checkIn).queryParam("checkOut", checkOut)
                .queryParam("guests", 2).queryParam("rooms", 1)
                .get("/hotels").then().statusCode(200).body("size()", org.hamcrest.Matchers.greaterThan(0));
    }

    @Test
    void theOrderAmountComesFromTheServerAndASoldOutRoomIsRefused() {
        given().contentType(ContentType.JSON).body(stay(HARBOUR, 4, DELUXE, EXECUTIVE))
                .post("/payments/orders").then().statusCode(200)
                .body("amountPaise", is(3_480_000)).body("orderId", startsWith("order_"));

        given().contentType(ContentType.JSON).body(stay(HARBOUR, 4, SUITE))
                .post("/payments/orders").then().statusCode(409).body("code", is("ROOM_UNAVAILABLE"));
    }

    @Test
    void twoRoomsAreOneBookingOnePaymentOneEmailAndANewAccount() { // AC8-14
        book(checkout("Asha Rao", "New.Guest@Example.com", "pay_two_rooms", HARBOUR, DELUXE, EXECUTIVE))
                .then().statusCode(201)
                .body("reference", matchesPattern("^HB-[A-Z0-9]{6}$"))
                .body("accountCreated", is(true));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking_room", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT email FROM guest_account", String.class)).isEqualTo("new.guest@example.com");
        assertThat(jdbc.queryForObject("SELECT to_address FROM email_outbox WHERE status = 'PENDING'", String.class))
                .isEqualTo("new.guest@example.com");
        assertThat(jdbc.queryForObject(
                        "SELECT available FROM room_inventory WHERE room_type_id = ? AND night = ?",
                        Integer.class, DELUXE, java.sql.Date.valueOf(checkIn)))
                .isEqualTo(3);
    }

    @Test
    void aBookingIsLinkedToAnExistingAccountIgnoringEmailCase() { // AC15
        jdbc.update("INSERT INTO guest_account (email) VALUES ('priya@example.com')");

        String reference = book(checkout("Priya", "Priya@Example.com", "pay_priya", HARBOUR, DELUXE))
                .then().statusCode(201).body("accountCreated", is(false)).extract().path("reference");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_account", Integer.class)).isEqualTo(1);
        given().get("/bookings/{ref}", reference).then().statusCode(200)
                .body("reference", is(reference)).body("email", is("priya@example.com"))
                .body("rooms.size()", is(1));
    }

    @Test
    void aBlankNameMakesNoBooking() { // AC6
        book(checkout("", "a@example.com", "pay_blank", HARBOUR, DELUXE))
                .then().statusCode(400).body("fieldErrors.name", is("Enter your full name"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment", Integer.class)).isZero();
    }

    @Test
    void twoGuestsBookingTheLastRoomGetOneBookingAndOneRoomTaken() throws Exception { // AC16
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> attempts = List.of(
                    () -> book(checkout("Guest One", "one@example.com", "pay_race_1", MARINE, MARINE_ROOM)).statusCode(),
                    () -> book(checkout("Guest Two", "two@example.com", "pay_race_2", MARINE, MARINE_ROOM)).statusCode());
            var statuses = pool.invokeAll(attempts).stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM payment WHERE status = 'CAPTURED'", Integer.class))
                    .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class));
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void aChargedCardWithAnUnsavableBookingIsReversedAndReportedAsFailed() { // AC18, AC19
        doThrow(new IllegalStateException("outbox down")).when(outbox).enqueue(any(), any());

        book(checkout("Asha Rao", "asha@example.com", "pay_fail", HARBOUR, DELUXE))
                .then().statusCode(502).body("code", is("BOOKING_FAILED"));

        assertThat(jdbc.queryForObject("SELECT status FROM payment WHERE provider_ref = 'pay_fail'", String.class))
                .isEqualTo("REFUNDED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_outbox", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT available FROM room_inventory WHERE room_type_id = ? AND night = ?",
                        Integer.class, DELUXE, java.sql.Date.valueOf(checkIn)))
                .isEqualTo(4);
    }

    @Test
    void theSchemaRefusesToDeleteWhatABookingDependsOn() {
        book(checkout("Asha Rao", "keep@example.com", "pay_keep", HARBOUR, DELUXE)).then().statusCode(201);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM guest_account")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM hotel WHERE id = ?", HARBOUR))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM payment")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theLoaderIsIdempotentAndNeverResetsInventoryUsedByABooking() throws Exception {
        int hotels = jdbc.queryForObject("SELECT count(*) FROM hotel", Integer.class);
        book(checkout("Asha Rao", "loader@example.com", "pay_loader", HARBOUR, DELUXE)).then().statusCode(201);

        runLoader();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM hotel", Integer.class)).isEqualTo(hotels);
        assertThat(jdbc.queryForObject(
                        "SELECT available FROM room_inventory WHERE room_type_id = ? AND night = ?",
                        Integer.class, DELUXE, java.sql.Date.valueOf(checkIn)))
                .isEqualTo(3);
        assertThat(Files.readString(LOADER).toUpperCase()).doesNotContain("DELETE");
    }
}
