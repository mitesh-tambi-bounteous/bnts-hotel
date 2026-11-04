-- Booking schema. All amounts are integer paise (INR only). Every foreign key is
-- ON DELETE RESTRICT: this story adds no delete path, so nothing a booking depends on can be removed.

-- Catalogue (owned by this service)
CREATE TABLE hotel (
    id      bigint PRIMARY KEY,
    brand   text   NOT NULL,
    city    text   NOT NULL,
    name    text   NOT NULL,
    summary text   NOT NULL
);
CREATE INDEX hotel_city_idx ON hotel (lower(city));

-- Only the tabs a hotel has a row for are ever shown.
CREATE TABLE hotel_tab (
    hotel_id bigint  NOT NULL REFERENCES hotel (id) ON DELETE RESTRICT,
    name     text    NOT NULL,
    body     text    NOT NULL,
    position integer NOT NULL,
    PRIMARY KEY (hotel_id, name)
);

CREATE TABLE room_type (
    id                    bigint  PRIMARY KEY,
    hotel_id              bigint  NOT NULL REFERENCES hotel (id) ON DELETE RESTRICT,
    name                  text    NOT NULL,
    bed                   text    NOT NULL,
    sleeps                integer NOT NULL CHECK (sleeps >= 1),
    price_per_night_paise bigint  NOT NULL CHECK (price_per_night_paise > 0)
);
CREATE INDEX room_type_hotel_idx ON room_type (hotel_id);

CREATE TABLE room_inventory (
    room_type_id bigint  NOT NULL REFERENCES room_type (id) ON DELETE RESTRICT,
    night        date    NOT NULL,
    available    integer NOT NULL CHECK (available >= 0),
    PRIMARY KEY (room_type_id, night)
);

-- Guests and bookings
CREATE TABLE guest_account (
    id    bigserial PRIMARY KEY,
    email text      NOT NULL UNIQUE CHECK (email = lower(email))
);

CREATE TABLE payment (
    id           bigserial PRIMARY KEY,
    provider_ref text      NOT NULL UNIQUE,
    amount_paise bigint    NOT NULL CHECK (amount_paise > 0),
    status       text      NOT NULL CHECK (status IN ('CAPTURED', 'REFUNDED'))
);

CREATE TABLE booking (
    id              bigserial   PRIMARY KEY,
    reference       text        NOT NULL UNIQUE,
    account_id      bigint      NOT NULL REFERENCES guest_account (id) ON DELETE RESTRICT,
    hotel_id        bigint      NOT NULL REFERENCES hotel (id) ON DELETE RESTRICT,
    check_in        date        NOT NULL,
    check_out       date        NOT NULL,
    guests          integer     NOT NULL CHECK (guests >= 1),
    total_paise     bigint      NOT NULL CHECK (total_paise > 0),
    payment_id      bigint      NOT NULL UNIQUE REFERENCES payment (id) ON DELETE RESTRICT,
    guest_name      text        NOT NULL,
    guest_phone     text,
    account_created boolean     NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CHECK (check_out > check_in)
);
CREATE INDEX booking_account_idx ON booking (account_id);

CREATE TABLE booking_room (
    booking_id            bigint NOT NULL REFERENCES booking (id) ON DELETE RESTRICT,
    room_type_id          bigint NOT NULL REFERENCES room_type (id) ON DELETE RESTRICT,
    price_per_night_paise bigint NOT NULL
);
CREATE INDEX booking_room_booking_idx ON booking_room (booking_id);

-- Transactional outbox, written with the booking and read by the Booking Worker.
CREATE TABLE email_outbox (
    id              bigserial   PRIMARY KEY,
    to_address      text        NOT NULL,
    payload         jsonb       NOT NULL,
    status          text        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts        integer     NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    last_error      text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz
);
CREATE INDEX email_outbox_due_idx ON email_outbox (status, next_attempt_at);
