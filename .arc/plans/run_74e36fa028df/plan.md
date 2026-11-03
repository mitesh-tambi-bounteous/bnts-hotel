summary: |
  Build the guest happy path end to end: Hotel search (city, dates, guests, rooms) -> Hotel detail (catalogue-defined tabs, room cards with Select room / Sold out / Doesn't suit) -> Checkout (name + email required, phone optional, one card payment for all rooms) -> Booking confirmed (one booking reference) plus a confirmation email. The Booking API (Spring Boot, PostgreSQL) gains search, hotel-detail, and checkout endpoints; checkout re-checks availability, takes one payment, saves one booking with N rooms linked to the account for the checkout email (created or existing), queues the confirmation email in the same database transaction (a transactional outbox row), and reverses the payment if the save fails. Card payment is taken through Razorpay (server-created order, Razorpay Checkout in the browser, server-side signature verification, Razorpay refund on failure) and the Booking Worker sends the queued confirmation email asynchronously through Amazon SES in ap-south-1, retrying on failure. Local and dev start without Razorpay keys or an SES sender (in-memory and logging adapters); only the qa and prod profiles refuse to start when they are blank. Guest Web (Next.js, Tailwind, design tokens) renders the four approved screens plus the room-taken and booking-failed states. This story also owns the catalogue and guest account tables and deletes no data (a guest's personal data is deleted only on request, soft-deleted for 1 year then permanently, which is a proposed follow-up). The no-results state (STORY-018), declined-card recovery, and My bookings listing are out of scope.
scope:
  - description: |
      Guest Web: load the design system once. globals.css today only has `@import "tailwindcss";` and layout.tsx hard-codes `bg-white text-slate-900`. Add imports of design-system/tokens.css (real token names: --color-*, --space-1..6, --radius-*, --size-content, --border-width-strong ...) and of the `.btn/.card/.field/.alert/.badge/.tabs/.stack/.grid/.skeleton/.spinner` component classes (these live in design-system/prototype-utils.css, whose header reads "Utilities and standard components. Built only from var(--token) references"). Make layout.tsx the shell from the guidelines: brand header "Guest Web", a single centred `<main>` capped at var(--size-content) padded with var(--space-5), and a footer; no nav.
    files:
      - apps/guest-web/src/app/globals.css
      - apps/guest-web/src/app/layout.tsx
    rationale: |
      The app does not load the tokens yet, so every later UI step depends on this. Needs a vitest check that the stylesheet imports tokens.
  - description: |
      Booking API schema. Add Flyway (spring.jpa.hibernate.ddl-auto is `none`, so nothing creates tables today) with V1 migration creating booking-owned tables `guest_account(id, email unique lower-cased)`, `booking(id, reference unique, account_id fk, hotel_id, check_in, check_out, guests, total_paise, payment_id, created_at)`, `booking_room(booking_id fk, room_type_id, price_per_night_paise)` `payment(id, provider_ref, amount_paise, status)` and `email_outbox(id bigserial, to_address, payload jsonb, status PENDING/SENT/FAILED, attempts int, next_attempt_at timestamptz, last_error, created_at, sent_at)` with an index on `(status, next_attempt_at)`, read by the Booking Worker, plus the catalogue tables, which this story now owns (reviewer decision): `hotel(id, brand, city, name, summary)`, `hotel_tab(hotel_id fk, name, body, position)` (only the tabs a hotel has a row for are ever shown), `room_type(id, hotel_id fk, name, bed, sleeps, price_per_night_paise)` and `room_inventory(room_type_id fk, night, available)`. This story adds no delete path: every foreign key is `ON DELETE RESTRICT` (no cascades), and no endpoint, job or repository method deletes rows. Deletion happens only on a guest's request (soft delete for 1 year, then permanent), which is a proposed follow-up (see the retention decision in open questions). A repeatable dev-only seed, `services/booking-api/src/main/resources/db/dev/R__dev_seed.sql`, loads the prototype's Mumbai hotels, rooms, tabs and the `priya@example.com` account with fixed ids and `INSERT ... ON CONFLICT DO NOTHING`; it is on the Flyway locations only under the `local` profile, set in `application-local.properties`.
    files:
      - services/booking-api/build.gradle
      - services/booking-api/src/main/resources/application.properties
      - services/booking-api/src/main/resources/db/migration/V1__booking_schema.sql
      - services/booking-api/src/main/resources/db/dev/R__dev_seed.sql
      - services/booking-api/src/main/resources/application-local.properties
    rationale: |
      Amounts are stored as integer paise (never floats); INR only. Email is stored lower-cased so existing-account matching is case-insensitive. Owning the catalogue and account tables here means there is a single V1 and no reconciliation with other stories; later catalogue or guest-bookings work extends these tables with new V-numbered migrations rather than rewriting V1. A refunded or failed payment keeps its `payment` row with status REFUNDED, and an inventory change is an UPDATE, never a delete, so this story removes nothing.
  - description: |
      Production catalogue loader (reviewer decision: a one-off script for now). `ops/load_catalogue.sql` is run by hand by an operator after V1 has been applied, for example `psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f services/booking-api/ops/load_catalogue.sql`; it is not part of Flyway and the app never runs it. It upserts `hotel`, `hotel_tab` and `room_type` rows by fixed id (`ON CONFLICT (id) DO UPDATE` for descriptive fields and price) and inserts `room_inventory` with `ON CONFLICT (room_type_id, night) DO NOTHING`, so re-running it can never reset availability that bookings have already used. It contains no DELETE, matching the no-delete rule. `ops/README.md` documents how to run it and what each section needs. Reviewer decision: until the business supplies real data, the script holds MOCK data, marked with a header comment saying so. It covers the six cities in the prototype's City select (Mumbai, Delhi, Bengaluru, Hyderabad, Chennai, Goa) with 2 to 4 invented hotels each. Mumbai uses the prototype's own hotels and prices: Harbour View Mumbai (Deluxe sea-facing room 7800, Executive room 9600, Harbour suite 14200 with zero inventory so it shows as sold out, Compact single room 4900 which sleeps 1 so it shows as unsuitable for 4 guests in 2 rooms), Bandra Court 6400, Airport Inn Andheri 4200 and Marine Drive Residency 9100, with room counts and capacities chosen so that 3 rooms or 9 guests drops some hotels (AC 20). Prices are stored in paise (rupees x 100). Tabs vary on purpose to exercise AC 23: Harbour View has Overview, Amenities and Location with no Policies tab, Bandra Court has all four, Airport Inn has Overview only. Inventory is generated for the next 90 nights from `current_date` with `generate_series`, so a re-run later adds new nights without touching existing ones.
    files:
      - services/booking-api/ops/load_catalogue.sql
      - services/booking-api/ops/README.md
    rationale: |
      Without it a production deployment has hotels to search for in no table. Keeping it out of Flyway means catalogue changes do not need a release. Inventory is DO NOTHING deliberately: an UPDATE there would silently re-open rooms that were booked.
  - description: |
      Booking API search + hotel detail (AC 1, 2, 3, 4, 20, 21, 22, 23). `GET /hotels?city=&checkIn=&checkOut=&guests=&rooms=` returns the brand's hotels in that city whose AVAILABLE and suitable rooms can jointly cover `rooms` rooms and `guests` guests, each with `fromPricePerNightPaise` (taxes included). `GET /hotels/{id}?checkIn=&checkOut=&guests=&rooms=` returns the hotel, only its catalogue-defined `tabs`, and rooms each with `status` of AVAILABLE, SOLD_OUT or UNSUITABLE and a `reason`.
    files:
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/search/HotelSearchController.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/search/HotelSearchService.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/search/RoomSuitability.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/catalogue/CatalogueReader.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/catalogue/JpaCatalogueReader.java
    rationale: |
      CatalogueReader is a port over the catalogue tables this story owns, kept so search logic is unit-testable without a database. Capacity rule: a room is UNSUITABLE when it cannot be part of any combination of `rooms` rooms that seats `guests` (assume each room must hold at least one guest, so sleeps >= 1 and sum(sleeps of the best `rooms` available rooms) >= guests).
  - description: |
      Booking API checkout (AC 6-19). `POST /bookings` with body `{hotelId, checkIn, checkOut, guests, roomTypeIds[], name, email, phone?, razorpayOrderId, razorpayPaymentId, razorpaySignature}`; the order itself comes from a preceding `POST /payments/orders` (see the Razorpay scope item). Signature: `BookingResult BookingService.book(CheckoutCommand cmd)`. Flow: validate (blank name/email -> 400 with field errors, no payment, no row); verify availability -> 409 ROOM_UNAVAILABLE; `PaymentGateway.charge(long totalPaise, PaymentProof proof)` ONCE for the whole booking (for Razorpay: verify the signature and that the paid amount equals totalPaise); transaction: conditional inventory decrement (`UPDATE room_inventory SET available = available - 1 WHERE ... AND available > 0`, 0 rows -> refund + 409 ROOM_UNAVAILABLE), find-or-create `guest_account` by lower-cased email, insert one `booking` with one generated reference and N `booking_room` rows; on any other exception call `PaymentGateway.refund(paymentId)` and return 502 BOOKING_FAILED. In the SAME transaction as the booking insert, `void EmailOutbox.enqueue(ConfirmationEmailPayload p)` inserts a PENDING `email_outbox` row (to = the checkout email; payload = guest first name, reference, hotel name, check-in, check-out, nights, room lines, total in paise), a snapshot so the worker needs no joins; an enqueue failure therefore rolls the booking back and triggers the refund. The request never waits for SES. `PaymentGateway` gets an in-memory adapter here for tests and local dev; the Razorpay adapter is the next scope item and the email dispatcher is the Booking Worker scope item. `GET /bookings/{reference}` returns the confirmation view including `accountCreated` boolean.
    files:
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/booking/BookingController.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/booking/BookingService.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/booking/CheckoutCommand.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/booking/BookingReferenceGenerator.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/booking/ApiErrors.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/account/GuestAccountRepository.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/PaymentGateway.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/InMemoryPaymentGateway.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/email/EmailOutbox.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/email/ConfirmationEmailPayload.java
    rationale: |
      Charge happens after the availability pre-check and before the save so the card reversal path (AC 18) is real. The conditional UPDATE is the race guard for AC 16 (two guests, last room). Card numbers never reach this API: the browser sends only Razorpay's order, payment id and signature (ADR-0015 card data / PCI scope), which departs from the prototype's raw card inputs (see open questions).
  - description: |
      Real provider adapters. Razorpay (`com.razorpay:razorpay-java`): `RazorpayPaymentGateway implements PaymentGateway` behind a thin `RazorpayApi` interface (createOrder, fetchPayment, refund) so tests can stub the SDK. `POST /payments/orders` body `{hotelId, checkIn, checkOut, guests, roomTypeIds[]}` re-runs the availability check, computes the total server-side (never trusts a client amount), creates a Razorpay order in paise (currency INR, receipt = internal id) and returns `{orderId, amountPaise, keyId}`. `charge` verifies `HMAC_SHA256(orderId + "|" + paymentId, keySecret)` equals the signature (constant-time compare), fetches the payment and requires status `captured` (order created with auto-capture on), `order_id` match and `amount == totalPaise`. `refund(paymentId)` calls Razorpay's full refund with an idempotency receipt. Amazon SES is used only by the Booking Worker (next scope item), never by the API. Guest Web: the card section of `checkout-form.tsx` is replaced by a "Pay" step that loads Razorpay Checkout (`https://checkout.razorpay.com/v1/checkout.js`, loaded with next/script) using the `orderId` and `keyId` from the order endpoint, prefilled with the guest's name, email and phone, and passes the returned `razorpay_order_id`, `razorpay_payment_id`, `razorpay_signature` to the `submitCheckout` Server Action. Blank name or email is validated BEFORE the order is created or Razorpay opens, so no payment is possible (AC 6, 7). Provider selection follows the environment. With RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET both set, `RazorpayPaymentGateway` is used. With either blank in local or dev, the app still starts on `InMemoryPaymentGateway`: a WARN is logged at startup, `/payments/orders` returns a fake order with an empty `keyId`, and the web form then skips Razorpay Checkout and submits a fake proof, so the whole flow can be tried without charging anyone. In the `qa` and `prod` Spring profiles a blank key stops startup.
    files:
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/RazorpayPaymentGateway.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/RazorpayApi.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/RazorpaySignature.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/PaymentProof.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/PaymentOrderController.java
      - services/booking-api/src/main/java/com/bounteous/hotel/bookingapi/payment/ProviderConfig.java
      - services/booking-api/.env.example
      - apps/guest-web/src/components/razorpay-checkout.tsx
      - apps/guest-web/.env.example
    rationale: |
      `ProviderConfig` (a `@Configuration` taking `Environment`) picks the gateway bean from the keys and, when `env.acceptsProfiles(Profiles.of("qa", "prod"))`, throws IllegalStateException at startup if either key is blank. Only those two profiles are strict, so deployments MUST set SPRING_PROFILES_ACTIVE to `qa` or `prod` (see open questions). Keys come from the environment only and are never logged. No provider property is needed, and with no keys set the existing StatusControllerTest and BookingApiE2ETest still boot on the in-memory gateway. The .env.example of the API gains the two Razorpay variables with a comment that blank is fine locally.
  - description: |
      Booking Worker asynchronous confirmation email (AC 12, 13). The worker currently has only the Spring Boot starters (build.gradle lists starter, web and actuator; BookingWorkerApplication is a plain @SpringBootApplication), no database access and no scheduling. Add `spring-boot-starter-jdbc` and runtimeOnly `org.postgresql:postgresql` (both Boot-managed versions) plus `software.amazon.awssdk:sesv2`, and `@EnableScheduling` on BookingWorkerApplication. `EmailOutboxDispatcher` runs on `@Scheduled(fixedDelayString = "${email.outbox.poll-ms:5000}")`:
      ```java
      @Scheduled(fixedDelayString = "${email.outbox.poll-ms:5000}")
      public void dispatch()   // claims up to 10 due rows, sends each, records the outcome
      ```
      The claim is one statement that also leases the rows so several worker instances never send the same row: `UPDATE email_outbox SET next_attempt_at = now() + interval '2 minutes' WHERE id IN (SELECT id FROM email_outbox WHERE status = 'PENDING' AND next_attempt_at <= now() ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED) RETURNING *`. For each row it renders `ConfirmationEmailTemplate.render(payload)` and calls `EmailSender.send(new EmailMessage(to, subject, text, html))`, then sets status SENT and sent_at. On an exception it increments attempts, stores last_error, and sets next_attempt_at with exponential backoff (1, 2, 4, 8 minutes); after `email.outbox.max-attempts` (default 5) the status becomes FAILED and an ERROR is logged. Delivery is at-least-once. The email content follows the prototype's "Confirmation email" screen: subject "Your stay at {hotel} is booked", "Hi {first name}, your rooms are ready for you.", Booking reference, Dates ("Check-in 12 Dec 2026, check-out 14 Dec 2026 (2 nights)"), Rooms with per-room amounts, "Total paid, taxes included", and "Show this reference at the front desk when you arrive." `SesEmailSender` (SesV2Client, region from AWS_REGION default ap-south-1, From = SES_FROM_ADDRESS) is the real sender. With SES_FROM_ADDRESS blank in local or dev the worker still starts on `LoggingEmailSender`, which logs the rendered email at INFO and a WARN at startup; in `qa` and `prod` a blank value stops startup (same Environment profile rule as the API).
    files:
      - services/booking-worker/build.gradle
      - services/booking-worker/src/main/resources/application.properties
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/BookingWorkerApplication.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/EmailOutboxDispatcher.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/EmailOutboxRepository.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/ConfirmationEmailPayload.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/ConfirmationEmailTemplate.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/EmailSender.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/SesEmailSender.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/LoggingEmailSender.java
      - services/booking-worker/src/main/java/com/bounteous/hotel/bookingworker/email/EmailConfig.java
      - services/booking-worker/.env.example
    rationale: |
      A transactional outbox was chosen because email is asynchronous: the booking and its email request commit or roll back together, and the email survives an API crash or an SES outage with retries. The worker owns no schema (stack note), so the `email_outbox` table is created by the API's V1 migration; the worker reads and updates it with JdbcTemplate rather than sharing JPA entities. The payload JSON is a contract between two separately built Gradle projects, so both ConfirmationEmailPayload records have a JSON round-trip test on the same fixture. The worker's `test` task stays free of Docker (mock repository and SesV2Client); the claim query and the lease run in the Testcontainers `e2e` source set, which applies the API's V1 SQL to a PostgreSQL container. Neither services/booking-worker/.env.example nor apps/guest-web/.env.example exists today (I searched for `.env.example` under the repo; only services/booking-api/.env.example was found), so both are created.
  - description: |
      Guest Web data layer. `lib/api.ts` (server-side fetch wrapper over BOOKING_API_URL), `lib/money.ts` (`formatRupees(paise)` using Intl en-IN INR, no decimals), `lib/search-params.ts` (parse/validate city, dates, guests, rooms), and `lib/checkout-validation.ts` (`validateCheckout({name,email}) -> {name?: string, email?: string}` returning the design's messages "Enter your full name" / "Enter your email address").
    files:
      - apps/guest-web/src/lib/api.ts
      - apps/guest-web/src/lib/money.ts
      - apps/guest-web/src/lib/search-params.ts
      - apps/guest-web/src/lib/checkout-validation.ts
      - apps/guest-web/src/lib/types.ts
    rationale: |
      Pure functions first so most UI behaviour is unit-testable in vitest without a browser.
  - description: |
      Guest Web screens, from the approved prototype. (1) Hotel search `/` (page.tsx): "Find your stay" h1, `.search-bar` grid form with City select, Dates input, Guests select, Rooms select and a `.btn-primary` "Search hotels"; results section "Hotels in {city}" with a `.grid` of hotel cards (`.card.stack-sm`: image placeholder 4/3 on --color-bg-subtle, h3 name, muted summary, "Per night, taxes included" + `₹` price in --font-size-lg bold, `.btn-secondary` "View rooms"), `.skeleton` cards while loading (use loading.tsx), and a `role="status"` note "N hotels with R rooms for G guests." (2) Hotel detail `/hotels/[id]`: `.btn-ghost` "Back to results"; h1 + meta line; `.tabs` rendered ONLY from API `tabs`; "Rooms for your stay" `.card.room-card` list: Available shows `.btn-primary` "Select room" (selected: --border-width-strong solid --color-primary, `.badge-success` "Added to booking", `.btn-secondary` "Remove room"); SOLD_OUT shows `.badge-danger` "Sold out" + "No rooms left for your dates." and no button; UNSUITABLE shows `.badge-warning` "Doesn't suit your search" + reason and no button; both on --color-bg-subtle with muted h3. Selection tray (`aria-live="polite"`, --border-width-strong --color-primary border, --radius-lg, --space-3 padding) with per-room lines, total, `.btn-primary` "Continue to checkout" (disabled with no rooms). Room-taken `.alert.alert-danger` "That room is no longer available" at top when `?taken=` is present. (3) Checkout `/checkout`: `.btn-ghost` "Back to rooms", h1, `.alert-danger` booking-failed ("We couldn't complete your booking ... reversed in full"), `.alert-danger.error-summary` (takes focus, links to fields), summary `.card`, Guest details `.field`s (Full name, Email with hint, Phone number (optional)) using `.is-invalid` + `.field-error`, Pay by card section, and one `.btn-primary.btn-lg` "Pay ₹total" that shows `.spinner` and disables while pending; submission via a Server Action `submitCheckout`. (4) Booking confirmed `/booking/[reference]`: `.alert-success` "Your rooms are booked", `.reference` block (--font-size-xl bold, letter-spacing, --color-bg-subtle) holding the single reference, hotel `.card` with dates, room lines, "Total paid, taxes included" and "One card payment for both rooms.", `.alert-info` account line (Account created / Added to your account), `.btn-ghost` "Search more hotels". The prototype's "Prototype controls" card, the in-app "View confirmation email" screen, and its hash-based `data-go` navigation are prototype-only and are NOT built; the email is the API-sent message.
    files:
      - apps/guest-web/src/app/page.tsx
      - apps/guest-web/src/app/loading.tsx
      - apps/guest-web/src/app/hotels/[id]/page.tsx
      - apps/guest-web/src/app/checkout/page.tsx
      - apps/guest-web/src/app/checkout/actions.ts
      - apps/guest-web/src/app/booking/[reference]/page.tsx
      - apps/guest-web/src/components/search-form.tsx
      - apps/guest-web/src/components/hotel-card.tsx
      - apps/guest-web/src/components/room-card.tsx
      - apps/guest-web/src/components/room-picker.tsx
      - apps/guest-web/src/components/selection-tray.tsx
      - apps/guest-web/src/components/checkout-form.tsx
      - apps/guest-web/src/components/booking-reference.tsx
      - apps/guest-web/src/components/hotel-tabs.tsx
    rationale: |
      Reuse check: the only existing catalogue component is welcome.tsx (a heading), which this story replaces on `/`; hotel-card, room-card, selection-tray and booking-reference are the new components the prototype header itself lists. Buttons, cards, fields, alerts, badges, tabs, skeleton and spinner reuse the design-system classes rather than new components. Mobile-first single column at 375px; spacing only via `.stack`, `.stack-lg`, `.cluster`.
  - description: |
      Retire the scaffold greeting that the new home page replaces: remove welcome.tsx, greeting.ts and their unit test, and update tests/e2e/home.spec.ts to the search page.
    files:
      - apps/guest-web/src/components/welcome.tsx
      - apps/guest-web/src/lib/greeting.ts
      - apps/guest-web/tests/unit/welcome.test.tsx
      - apps/guest-web/tests/e2e/home.spec.ts
    rationale: |
      Avoids leaving dead scaffold code. Confirm with the reviewer if welcome should instead be kept (open questions).
tests:
  - |
    AC1 (JUnit, services/booking-api/src/test/java/.../search/HotelSearchServiceTest.java) — write first:
    ```java
    var result = service.search(new SearchQuery("Mumbai", checkIn, checkOut, 2, 1));
    assertThat(result).extracting(HotelSummary::name).containsExactlyInAnyOrder("Harbour View Mumbai", "Bandra Court");
    ```
    plus e2e `given().queryParam("city","Mumbai")...when().get("/hotels").then().body("name", hasItem("Bandra Court"))`.
  - |
    AC2 — `assertThat(result.get(0).fromPricePerNightPaise()).isEqualTo(780_000L);` (taxes included in the stored price) and vitest `expect(formatRupees(780000)).toBe("₹7,800")` in tests/unit/money.test.ts; component test `expect(screen.getByText("Per night, taxes included")).toBeTruthy()` in hotel-card.test.tsx.
  - |
    AC3 (vitest, tests/unit/room-card.test.tsx) — `render(<RoomCard room={{...soldOut, status:"SOLD_OUT"}} />); expect(screen.getByText("Sold out")).toBeTruthy();`
  - |
    AC4 — same file: `expect(screen.queryByRole("button", { name: "Select room" })).toBeNull();`, with a mirrored API test `assertThat(detail.rooms().get(2).status()).isEqualTo(RoomStatus.SOLD_OUT)`.
  - |
    AC5 (tests/unit/room-picker.test.tsx) — click Select room then `expect(screen.getByText("Added to booking")).toBeTruthy(); expect(screen.getByRole("link", { name: "Continue to checkout" })).toBeTruthy();` and the tray total equals `₹15,600`... computed as price x nights.
  - |
    AC6 (vitest checkout-validation + checkout-form tests, JUnit BookingServiceTest) — `expect(validateCheckout({ name: "", email: "a@b.com" })).toEqual({ name: "Enter your full name" });` and API: `assertThatThrownBy(() -> service.book(cmdWith("", "a@b.com"))).isInstanceOf(ValidationException.class); verify(paymentGateway, never()).charge(anyLong(), any()); assertThat(bookingRepo.count()).isZero();`
  - |
    AC7 — `expect(validateCheckout({ name: "Asha", email: "" })).toEqual({ email: "Enter your email address" });` and the same API assertions with a blank email; the form test asserts `expect(screen.getByRole("alert")).toHaveFocus()`-style focus on the error summary via `document.activeElement`.
  - |
    AC8 — `var r = service.book(cmd("Asha Rao", "asha@example.com", null)); assertThat(r.reference()).isNotBlank();` and e2e `POST /bookings` without `phone` returns 201.
  - |
    AC9 — `verify(paymentGateway, times(1)).charge(eq(2 * 2 * 780_000L), any());` for a 2-room, 2-night booking.
  - |
    AC10 — `assertThat(bookingRepo.findByReference(r.reference()).rooms()).hasSize(2); assertThat(bookingRepo.count()).isEqualTo(1);`
  - |
    AC11 — `assertThat(bookingRepo.count()).isEqualTo(1)` with a single `reference` matching `^HB-[A-Z0-9]{6}$` (test in BookingReferenceGeneratorTest); web test `expect(screen.getAllByText("HB-7K2M9Q")).toHaveLength(1)` in booking-reference.test.tsx.
  - |
    AC12 — two halves. API (BookingServiceTest): the booking queues exactly one email for the checkout email and the request does not send it: `assertThat(outboxRepo.findPending()).singleElement().extracting(OutboxRow::toAddress).isEqualTo("asha@example.com");`. Worker (EmailOutboxDispatcherTest, mocked repository and sender): `dispatcher.dispatch(); verify(sender).send(argThat(m -> m.to().equals("asha@example.com"))); verify(repo).markSent(row.id());`.
  - |
    AC13 (worker, email/ConfirmationEmailTemplateTest) — `var body = template.render(payload); assertThat(body).contains(reference, "Deluxe sea-facing room", "Executive room", "12 Dec 2026", "14 Dec 2026", "₹31,200");` (adjust to the fixture total).
  - |
    AC14 (e2e, Testcontainers PostgreSQL) — `assertThat(accountRepo.findByEmail("new@example.com")).isPresent(); assertThat(accountRepo.findByEmail("new@example.com").get().bookings()).hasSize(1);` and `body("accountCreated", is(true))`.
  - |
    AC15 — seed `priya@example.com`, book with `Priya@Example.com`, then `assertThat(accountRepo.count()).isEqualTo(1); assertThat(booking.accountId()).isEqualTo(existing.id()); ` and `body("accountCreated", is(false))`.
  - |
    AC16 — concurrent e2e: two threads book the last room; `assertThat(statuses).containsExactlyInAnyOrder(201, 409);` and loser `body("code", is("ROOM_UNAVAILABLE"))`; no payment remains for the loser (`verify(paymentGateway, never()).charge` for pre-check loser, or `refund` called when the conditional UPDATE loses).
  - |
    AC17 (vitest, checkout-actions.test.ts) — action given a 409 ROOM_UNAVAILABLE calls `redirect(`/hotels/${hotelId}?taken=${roomTypeId}`)`; page test `expect(screen.getByText("That room is no longer available")).toBeTruthy()` and the taken room is not preselected.
  - |
    AC18 — make `bookingRepo.save` throw: `assertThatThrownBy(() -> service.book(cmd)).isInstanceOf(BookingFailedException.class); verify(paymentGateway).refund(paymentId);`
  - |
    AC19 — e2e: `statusCode(502).body("code", is("BOOKING_FAILED"))`; web test `expect(screen.getByText("We couldn't complete your booking")).toBeTruthy()`.
  - |
    AC20 — `assertThat(service.search(new SearchQuery("Mumbai", in, out, 9, 3))).extracting(HotelSummary::name).doesNotContain("Airport Inn Andheri");` (2 rooms / 6 guests capacity vs 3 rooms and 9 guests).
  - |
    AC21 — `assertThat(roomByName(detail, "Compact single room").status()).isEqualTo(RoomStatus.UNSUITABLE);` for 4 guests in 2 rooms; web `expect(screen.getByText("Doesn't suit your search")).toBeTruthy()`.
  - |
    AC22 — `expect(screen.queryByRole("button", { name: "Select room" })).toBeNull();` for an UNSUITABLE room card.
  - |
    AC23 — hotel with tabs [Overview, Amenities, Location]: `expect(screen.getAllByRole("tab").map(t => t.textContent)).toEqual(["Overview","Amenities","Location"]); expect(screen.queryByRole("tab", { name: "Policies" })).toBeNull();` and API `assertThat(detail.tabs()).extracting(Tab::name).containsExactly("Overview","Amenities","Location");`
  - |
    Razorpay adapter (JUnit, payment/RazorpayPaymentGatewayTest.java and RazorpaySignatureTest.java) — a signature computed with the key secret is accepted, a tampered one is rejected, and a wrong amount is rejected before any booking is saved:
    ```java
    assertThat(RazorpaySignature.verify("order_1", "pay_1", sign("order_1|pay_1", SECRET), SECRET)).isTrue();
    assertThat(RazorpaySignature.verify("order_1", "pay_2", sign("order_1|pay_1", SECRET), SECRET)).isFalse();
    when(api.fetchPayment("pay_1")).thenReturn(new RazorpayPayment("captured", "order_1", 1_000L));
    assertThatThrownBy(() -> gateway.charge(2_000L, proof)).isInstanceOf(PaymentVerificationException.class);
    ```
    and `gateway.refund("pay_1"); verify(api).refund("pay_1");` (supports AC 9 and 18).
  - |
    Order endpoint (e2e, RestAssured) — the total comes from the server, not the client: `given().body(orderRequest2Rooms2Nights).post("/payments/orders").then().statusCode(200).body("amountPaise", is(3_120_000)).body("orderId", startsWith("order_"));` and a sold-out room returns 409 `ROOM_UNAVAILABLE` with no order created (`verify(api, never()).createOrder(anyLong(), any())`).
  - |
    SES adapter (worker JUnit, email/SesEmailSenderTest.java, SesV2Client mocked) — `verify(ses).sendEmail(argThat((SendEmailRequest r) -> r.destination().toAddresses().contains("asha@example.com") && r.content().simple().body().text().data().contains(reference)));`
  - |
    Provider config (JUnit, payment/ProviderConfigTest.java) — blank keys stop startup only in qa and prod:
    ```java
    new ApplicationContextRunner().withUserConfiguration(ProviderConfig.class)
        .withPropertyValues("spring.profiles.active=prod", "razorpay.key-id=", "razorpay.key-secret=")
        .run(ctx -> assertThat(ctx).hasFailed());
    new ApplicationContextRunner().withUserConfiguration(ProviderConfig.class)
        .withPropertyValues("razorpay.key-id=", "razorpay.key-secret=")
        .run(ctx -> assertThat(ctx).hasSingleBean(InMemoryPaymentGateway.class));
    ```
    (the `qa` profile gets the same failing case; a mirrored EmailConfigTest in the worker does the same for a blank SES_FROM_ADDRESS, expecting `LoggingEmailSender` outside qa and prod). The web fallback is tested too: when the order response has an empty `keyId`, `expect(openCheckout).not.toHaveBeenCalled(); expect(submitCheckout).toHaveBeenCalled();`. A second vitest case in razorpay-checkout.test.tsx covers AC 6 and 7: `expect(openCheckout).not.toHaveBeenCalled()` when name or email is blank.
  - |
    Worker retry and failure (JUnit, EmailOutboxDispatcherTest, mocked sender throwing) — a failed send is rescheduled with backoff and a final failure is recorded, and neither touches the booking:
    ```java
    doThrow(new EmailSendException("ses down")).when(sender).send(any());
    dispatcher.dispatch();
    verify(repo).scheduleRetry(eq(row.id()), eq(1), any(Instant.class), contains("ses down"));
    // attempt 5 of 5
    dispatcher.dispatch();
    verify(repo).markFailed(eq(row.id()), contains("ses down"));
    verify(repo, never()).markSent(anyLong());
    ```
  - |
    Outbox claim and lease (worker Testcontainers e2e, `(cd services/booking-worker && gradle e2e --no-daemon)`, V1 SQL applied to the container) — two dispatchers racing never claim the same row, and a row still leased is skipped:
    ```java
    var claimed = IntStream.range(0, 2).parallel().mapToObj(i -> repo.claimDue(10)).flatMap(List::stream).toList();
    assertThat(claimed).extracting(OutboxRow::id).doesNotHaveDuplicates().hasSize(5);
    assertThat(repo.claimDue(10)).isEmpty();
    ```
    plus a payload contract test run in both projects against the same JSON fixture: `assertThat(mapper.readValue(FIXTURE, ConfirmationEmailPayload.class).reference()).isEqualTo("HB-7K2M9Q");`.
  - |
    This story deletes nothing (Testcontainers e2e, booking-api) — the schema refuses deletes of anything a booking depends on, and a refunded payment is kept:
    ```java
    assertThatThrownBy(() -> jdbc.update("DELETE FROM guest_account WHERE id = ?", accountId)).isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM hotel WHERE id = ?", hotelId)).isInstanceOf(DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("SELECT status FROM payment WHERE id = ?", String.class, paymentId)).isEqualTo("REFUNDED");
    ```
    plus a source check that no repository or service exposes a delete: `assertThat(Arrays.stream(BookingService.class.getMethods()).map(Method::getName)).noneMatch(n -> n.startsWith("delete"));`.
  - |
    Migration and seed (Testcontainers e2e) — V1 applies to an empty PostgreSQL and the app starts with ddl-auto none; with the `local` profile the seed loads and running it twice changes nothing: `assertThat(count("hotel")).isEqualTo(countAfterSecondRun("hotel"));` and `assertThat(search("Mumbai")).isNotEmpty();`. The seed is not on the Flyway locations without the `local` profile: `assertThat(count("hotel")).isZero();`.
  - |
    Catalogue loader (Testcontainers e2e, booking-api) — the script runs against the V1 schema, is idempotent, and never resets inventory used by a booking:
    ```java
    runScript("ops/load_catalogue.sql");
    book(roomTypeId, night);                       // available 3 -> 2
    runScript("ops/load_catalogue.sql");           // second run
    assertThat(count("hotel")).isEqualTo(hotelsAfterFirstRun);
    assertThat(available(roomTypeId, night)).isEqualTo(2);
    assertThat(Files.readString(Path.of("ops/load_catalogue.sql")).toUpperCase()).doesNotContain("DELETE");
    ```
  - |
    Mock data exercises every state (Testcontainers e2e, booking-api, after running ops/load_catalogue.sql) — Mumbai returns the prototype's hotels, the suite is sold out, the compact single room is unsuitable, and the tab sets differ:
    ```java
    var mumbai = search("Mumbai", twoRooms, fourGuests);
    assertThat(mumbai).extracting(HotelSummary::name).contains("Harbour View Mumbai", "Bandra Court");
    var detail = hotel("Harbour View Mumbai", twoRooms, fourGuests);
    assertThat(room(detail, "Harbour suite").status()).isEqualTo(RoomStatus.SOLD_OUT);
    assertThat(room(detail, "Compact single room").status()).isEqualTo(RoomStatus.UNSUITABLE);
    assertThat(detail.tabs()).extracting(Tab::name).containsExactly("Overview", "Amenities", "Location");
    assertThat(search("Goa", oneRoom, twoGuests)).isNotEmpty();
    ```
  - |
    Design system loading (vitest, tests/unit/globals-css.test.ts) — `expect(readFileSync("src/app/globals.css","utf8")).toContain("tokens.css");`. Playwright happy-path e2e (apps/guest-web/tests/e2e/booking.spec.ts) walks search -> detail -> checkout -> confirmation against a stubbed API. Narrow commands: `(cd services/booking-api && gradle test --tests '*BookingServiceTest')` and `(cd apps/guest-web && pnpm test tests/unit/room-card.test.tsx)`.
assumptions_or_open_questions:
  - |
    Decided with the reviewer: Razorpay is the card payment provider (settles the EPIC-004 question). Test environment uses Razorpay test-mode keys and production uses live keys, both supplied as RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables; the human must create the Razorpay account and supply the keys. Deliberate departure from the prototype: its raw card number, name, expiry and CVC inputs are NOT built, because Razorpay Checkout collects card details in its own hosted UI (keeps card data out of our systems, ADR-0015). The prototype's "Pay by card" section becomes a note plus the Razorpay popup opened by the single "Pay" button. The reviewer confirmed this change from the prototype is accepted.
  - |
    Not planned, flagged for a follow-up: a Razorpay webhook (payment.captured) to reconcile a guest who pays but closes the browser before the booking is submitted. Without it, such a payment would have no booking and would need manual refund. Also whether Razorpay payments should be authorised then captured after the booking is saved, which would remove most refunds; the plan assumes auto-capture to match AC 18 (charged, then reversed).
  - |
    Decided with the reviewer: this story owns the catalogue tables (hotel, hotel_tab, room_type, room_inventory) and the guest account table, so there is no reconciliation step. services/booking-api contains only StatusController today, so nothing else defines them. No admin screens or endpoints for catalogue data are planned (the staff admin app is untouched). Reviewer decision: for now production hotels, rooms, tabs and inventory are loaded by a one-off script, which this plan includes as a run-by-hand SQL file (see the scope item for ops/load_catalogue.sql). Reviewer decision: use mock data for now wherever real data is missing, so the loader and the dev seed both hold invented hotels, rooms, prices, tabs and inventory (the Mumbai ones copied from the prototype). The business must replace the mock data with real hotels, rooms, prices and inventory before go-live; because the loader upserts by fixed id and never deletes, the mock rows would have to be removed or hidden by hand (for example with a `listed` flag in a later migration), which is not planned here. The dev seed only runs under the `local` profile. Other mock stand-ins already in the plan: the in-memory payment gateway and the logging email sender when keys are blank, and the grey image placeholder on hotel cards from the prototype (no real photos).
  - |
    Decided with the reviewer (answers the deferred retention question): data is kept indefinitely, and a guest's personal data is deleted only on the guest's request, soft-deleted for 1 year and then permanently deleted. Further decisions from the reviewer: (a) at the end of the year the personal data is ANONYMISED, not deleted, so booking and payment rows are kept; (b) the year runs from the erasure request; (c) for now a guest asks through the backend only, with no screen. This story builds none of the erasure machinery (no AC, no prototype screen); it only avoids blocking it: no delete or purge code, ON DELETE RESTRICT on every foreign key, refunded payments and sent emails kept as rows with a status, and a test that the database refuses to delete a booked account or hotel. Proposed follow-up story (not in this plan, awaiting confirmation that it is a separate story): (1) a backend erasure-request endpoint that sets `guest_account.deleted_at` (also the start of the 1-year clock) and hides the account from lookups; (2) a scheduled job in the Booking Worker that, once `deleted_at` is more than 1 year old, irreversibly anonymises the personal fields in `guest_account` (name, email, phone), in the guest's bookings, and in `email_outbox.to_address` and payload. It would arrive as a later V-numbered migration. Still undecided for that follow-up: who may call the erasure endpoint and how the guest is authenticated, whether the same email may book again during the year (this story's find-or-create would otherwise attach a new booking to a soft-deleted account), and whether anonymisation or the soft delete is the step at the request.
  - |
    Decided with the reviewer: Amazon SES in ap-south-1 sends the confirmation email (ADR-0009, ADR-0013). Prerequisites outside the code, to be done by the human: verify the sender domain or address (SPF, DKIM), request SES production access, and grant the Booking Worker runtime role ses:SendEmail (the worker, not the API, sends). Email is asynchronous per the reviewer: an outbox row is written in the booking transaction and the Booking Worker polls it, so a send failure never reverses a booking. I chose the outbox over an in-process @Async send in the API because it survives a crash or an SES outage and gives retries; the cheaper alternative (an @Async send, no retry, nothing queued) was the alternative; the reviewer accepted the outbox. Delivery is at-least-once, so a rare duplicate email is possible, and the guest has no way to resend from the app.
  - |
    Local and dev start with blank Razorpay keys and SES sender; only the qa and prod Spring profiles fail startup (reviewer decision). This relies on qa and prod deployments setting SPRING_PROFILES_ACTIVE to exactly `qa` or `prod`: a deployment started with no profile would silently run the in-memory gateway and the logging sender, taking no payments and sending no emails. The reviewer confirmed `qa` and `prod` are the real profile names. Deployment configuration was not read, so whoever deploys must set SPRING_PROFILES_ACTIVE accordingly for both the Booking API and the Booking Worker.
  - |
    The worker needs the Booking API's database credentials (the same SPRING_DATASOURCE_* variables) and network access to the PostgreSQL instance; the Booking Worker stack note says it uses the API-owned database and owns no schema, which the plan follows.
  - |
    Scale from ADR-0006 and ADR-0007 (1,000 bookings per day, 1,000 concurrent users) is met by row-level conditional updates and indexed reads; since this story deletes nothing, `booking`, `payment` and `email_outbox` grow without bound. At 1,000 bookings per day that is modest, so no partitioning or archiving is planned.
  - |
    Room suitability rule assumed: a room is UNSUITABLE if the best `rooms` available rooms cannot seat the guests, or the room alone cannot be part of a seating combination; per-room guest split at checkout is not collected. Confirmed by the reviewer.
  - |
    Booking reference format `HB-` plus 6 characters from an unambiguous alphabet is inferred from the prototype's HB-7K2M9Q; assumed unique via a database constraint with retry. Confirmed by the reviewer.
  - |
    Nights and price: total = sum(room price per night x nights); taxes are included in the stored price (no separate tax line). Currency is INR only. Confirmed by the reviewer.
  - |
    Importing design-system/ CSS from outside apps/guest-web: Next.js/Tailwind may reject an `@import` that leaves the app root. If so, a small sync script copying the files into apps/guest-web/src/styles would be needed. Accepted by the reviewer; verify when implementing and use the copy script only if the import is rejected.
  - |
    The prototype's Dates field is a free-text input ("12 Dec 2026 to 14 Dec 2026"); the plan uses two date inputs (check-in, check-out) submitted as ISO dates so validation is exact. Confirmed by the reviewer: two date inputs, not the single field. No token covers the search-bar min column width (140px literal in the prototype), so the plan uses var(--size-grid-min); also confirmed.
estimated_points: 8
package_dependencies:
  - name: com.razorpay:razorpay-java
    version: ">=1.4.8,<2"
    ecosystem: maven
    rationale: |
      Official Razorpay Java SDK for creating orders, fetching payments and issuing refunds from the Booking API.
    component_id: comp_booking_api
  - name: software.amazon.awssdk:sesv2
    version: ">=2.29,<3"
    ecosystem: maven
    rationale: |
      AWS SDK v2 SES v2 client used by the Booking Worker to send the queued confirmation email in ap-south-1.
    component_id: comp_booking_worker
  - name: org.flywaydb:flyway-core
    version: ">=10.20,<11"
    ecosystem: maven
    rationale: |
      Booking API has ddl-auto=none and no schema mechanism; Flyway versioned migrations create the booking tables.
    component_id: comp_booking_api
  - name: org.flywaydb:flyway-database-postgresql
    version: ">=10.20,<11"
    ecosystem: maven
    rationale: |
      Flyway 10 moved PostgreSQL support into this module.
    component_id: comp_booking_api
notes: |
  ### Data & rollout

  - **Existing data**: None exists — the Booking API has no tables yet (ddl-auto=none, only StatusController). V1 creates new tables only and does not alter any existing ones; this story owns the catalogue and account tables, so no other migration defines them. All foreign keys are ON DELETE RESTRICT and this story's code deletes no rows. Per the reviewer, personal data is deleted only on a guest's request (soft delete for 1 year, then permanent deletion); that erasure flow is a proposed follow-up (open questions) and V1 does not yet contain its columns, so it will arrive as a later V-numbered migration (adding `guest_account.deleted_at`, nullable, which is safe for existing rows).
  - **Backfill / seed**: No backfill. The prototype's Mumbai hotels, rooms, tabs and the `priya@example.com` account are loaded by `db/dev/R__dev_seed.sql` only under the `local` profile (Flyway locations set in application-local.properties); it is idempotent (`ON CONFLICT DO NOTHING` on fixed ids) and safe to re-run. Tests load their own fixtures. Production has no seed: catalogue data must be loaded by other means (open questions).
  - **Settings** (documented in services/booking-api/.env.example, with the Spring datasource variables already there, and apps/guest-web/.env.example):
    - `BOOKING_API_URL` (Guest Web, server-side; default `http://localhost:8080` for dev).
    - `SPRING_PROFILES_ACTIVE`: `qa` or `prod` turn on strict startup checks (blank Razorpay keys, or a blank SES sender in the worker, stop the app). Any other value or none is local/dev, where blanks are allowed and in-memory/logging adapters are used with a WARN log.
    - `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`, no default; in `qa` and `prod` the Booking API refuses to start if either is blank; in local/dev it starts on the in-memory gateway. Secrets only from the environment or a secret store, never committed. The key id is returned to the browser in the order response, so Guest Web needs no Razorpay variable.
    - Booking Worker only, documented in the new services/booking-worker/.env.example: `AWS_REGION` default `ap-south-1`; `SES_FROM_ADDRESS`, no default (blank stops the worker in qa/prod, logging sender elsewhere); `EMAIL_OUTBOX_POLL_MS` default 5000; `EMAIL_OUTBOX_MAX_ATTEMPTS` default 5. AWS credentials come from the default provider chain (task role). The worker also needs the same SPRING_DATASOURCE_* variables as the API.
  - **Provisioning**: PostgreSQL (comp_relational_database) must be reachable with permission to run Flyway migrations. A Razorpay account with test and live keys; an SES verified sender in ap-south-1 with production access and an IAM permission ses:SendEmail on the Booking Worker role; the Booking Worker must be deployed and running, or confirmation emails stay queued. These are created by people or infrastructure, not by this change. The `email_outbox` table is the queue, created by V1. Production catalogue data is loaded by an operator running ops/load_catalogue.sql once after V1 (idempotent, safe to re-run). No bucket or feature flag.
  - **Order and rollback**: Deploy the Booking API (runs V1, creates email_outbox) before the Booking Worker and Guest Web. If the worker is down or not yet deployed, rows wait as PENDING and the emails go out when it starts. Rollback: the new tables are additive, so leaving them is safe; stop the worker to pause emails (rows stay PENDING), and reverting the web app alone hides the feature.

  Tests must also show the migration applies to an empty PostgreSQL container (Testcontainers) and that the app starts with `ddl-auto=none`.

  Package ecosystem: all four new dependencies are Maven coordinates (ecosystem maven), resolved through Gradle in services/booking-api and services/booking-worker.

  Tokens: today apps/guest-web/src/app/globals.css line 1 is only `@import "tailwindcss";` and layout.tsx line 13 uses `bg-white text-slate-900`; design-system/tokens.css is not loaded. The scope item adds that import once. Tokens used: --color-fg/-bg/-bg-subtle/-fg-muted, --color-primary, --color-border(-strong), --color-danger/-success/-warning/-info (+ -bg), --space-1..5, --radius-md/-lg, --font-size-sm/lg/xl, --font-weight-bold, --border-width(-strong), --size-content, --size-control-lg, --size-grid-min.

  ```mermaid
  flowchart TD
    Layout[layout.tsx + globals.css: shell and tokens]:::touched
    Search[app/page.tsx search]:::touched
    Detail[app/hotels/id/page.tsx]:::touched
    Checkout[app/checkout + actions.ts]:::touched
    Confirmed[app/booking/reference/page.tsx]:::touched
    Api[lib/api.ts]:::touched
    HSC[HotelSearchController]:::touched
    BC[BookingController]:::touched
    BS[BookingService]:::touched
    Cat[CatalogueReader port]:::touched
    Pay[PaymentGateway port]:::touched
    Mail[EmailOutbox.enqueue]:::touched
    Disp[EmailOutboxDispatcher in Booking Worker]:::touched
    DB[(PostgreSQL via Flyway V1)]:::touched
    Layout --> Search
    Layout --> Detail
    Layout --> Checkout
    Layout --> Confirmed
    Search --> Api
    Detail --> Api
    Checkout --> Api
    Confirmed --> Api
    Api -->|GET /hotels| HSC
    Api -->|POST /bookings| BC
    HSC --> Cat
    BC --> BS
    BS --> Cat
    BS -->|charge once, refund on failure| Pay
    BS -->|same transaction as the booking| Mail
    Pay -->|RazorpayPaymentGateway: verify signature, refund| RZ[Razorpay API]
    Mail -->|insert PENDING row| DB
    Disp -->|claim due rows, mark SENT or retry| DB
    Disp -->|SesEmailSender, ap-south-1| SES[Amazon SES]
    Checkout -->|POST /payments/orders| PO[PaymentOrderController]:::touched
    PO --> Pay
    BS --> DB
    Cat --> DB
    classDef touched fill:#f96,color:#000
  ```
review_focus: |
  IN: the four guest screens, the Booking API search/detail/checkout/confirmation endpoints, one payment and one booking per checkout, account link-or-create, confirmation email content and its asynchronous delivery (outbox row written with the booking, sent by the Booking Worker through SES with retries), the refund path and the room-taken path. OUT: no-results state, declined-card recovery, My bookings, a Razorpay webhook, a guest-facing resend, retention. Riskiest area: BookingService ordering (availability check, one charge, conditional inventory decrement, refund on any failure after the charge) and its race behaviour under concurrent bookings of the last room, plus Razorpay signature and amount verification (the server computes the total, never the client). Deliberate: this story owns the catalogue and account tables (reviewer decision, so no reconciliation with other stories), this story deletes no data (ON DELETE RESTRICT, no delete code, refunded payments kept as rows) and the on-request erasure with a 1-year soft delete is deliberately NOT built here, and the prototype's raw card inputs are replaced by Razorpay Checkout to keep card data out of scope (ADR-0015). The in-memory/logging adapters are used whenever Razorpay keys or the SES sender are blank, which is allowed in local/dev; only the qa and prod profiles fail startup, so a missing SPRING_PROFILES_ACTIVE in a real environment is the main configuration risk. The email outbox claim query (lease plus FOR UPDATE SKIP LOCKED) and the at-least-once delivery are deliberate.
