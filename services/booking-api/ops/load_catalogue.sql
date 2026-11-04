-- ============================================================================
-- MOCK DATA. Every hotel, room, price, tab and inventory figure below is invented
-- (Mumbai copies the approved prototype). Replace it with the business's real catalogue
-- before go-live. See ops/README.md.
--
-- Run by hand after Flyway V1 has been applied, never by the app:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f services/booking-api/ops/load_catalogue.sql
--
-- Safe to re-run: descriptive fields and prices are upserted by fixed id; inventory is only
-- inserted (DO NOTHING) so availability already used by bookings is never reset.
-- This script only inserts and updates; it removes nothing. Prices are integer paise (rupees x 100).
-- ============================================================================
BEGIN;

-- 1. Hotels ------------------------------------------------------------------
INSERT INTO hotel (id, brand, city, name, summary) VALUES
    (1,  'Hotel Brand', 'Mumbai',    'Harbour View Mumbai',        'Colaba, sea-facing rooms'),
    (2,  'Hotel Brand', 'Mumbai',    'Bandra Court',               'Bandra West, near the sea link'),
    (3,  'Hotel Brand', 'Mumbai',    'Airport Inn Andheri',        'Ten minutes from the airport'),
    (4,  'Hotel Brand', 'Mumbai',    'Marine Drive Residency',     'Marine Drive, promenade views'),
    (11, 'Hotel Brand', 'Delhi',     'Connaught Place Central',    'Connaught Place, metro at the door'),
    (12, 'Hotel Brand', 'Delhi',     'Aerocity Suites',            'Aerocity, five minutes from the airport'),
    (21, 'Hotel Brand', 'Bengaluru', 'Indiranagar Garden Hotel',   'Indiranagar, quiet leafy street'),
    (22, 'Hotel Brand', 'Bengaluru', 'MG Road Residency',          'MG Road, walk to the metro'),
    (23, 'Hotel Brand', 'Bengaluru', 'Whitefield Tech Stay',       'Whitefield, close to the tech parks'),
    (31, 'Hotel Brand', 'Hyderabad', 'Banjara Hills Palace',       'Banjara Hills, city views'),
    (32, 'Hotel Brand', 'Hyderabad', 'Hitech City Inn',            'Hitech City, business friendly'),
    (41, 'Hotel Brand', 'Chennai',   'Marina Beach Hotel',         'Beach Road, sea breeze'),
    (42, 'Hotel Brand', 'Chennai',   'T Nagar Comfort',            'T Nagar, steps from the shops'),
    (51, 'Hotel Brand', 'Goa',       'Calangute Beach Resort',     'Calangute, two minutes to the sand'),
    (52, 'Hotel Brand', 'Goa',       'Panjim Heritage House',      'Panjim, restored Portuguese house'),
    (53, 'Hotel Brand', 'Goa',       'Palolem Retreat',            'Palolem, quiet southern beach')
ON CONFLICT (id) DO UPDATE SET
    brand = EXCLUDED.brand, city = EXCLUDED.city, name = EXCLUDED.name, summary = EXCLUDED.summary;

-- 2. Tabs: a hotel only ever shows the tabs it has a row for ---------------------
INSERT INTO hotel_tab (hotel_id, name, body, position) VALUES
    (1, 'Overview',  'A restored seafront hotel in Colaba, a short walk from the Gateway of India. Breakfast is served on the terrace from 7am.', 1),
    (1, 'Amenities', 'Free Wi-Fi, rooftop pool, 24-hour front desk, airport transfers on request.', 2),
    (1, 'Location',  'Colaba Causeway, Mumbai 400005. Ten minutes by taxi from Chhatrapati Shivaji Terminus.', 3),
    (2, 'Overview',  'A calm hotel in Bandra West with a quiet courtyard and a short drive to the sea link.', 1),
    (2, 'Amenities', 'Free Wi-Fi, gym, in-house cafe, laundry service.', 2),
    (2, 'Location',  'Hill Road, Bandra West, Mumbai 400050. Ten minutes from Bandra station.', 3),
    (2, 'Policies',  'Check-in from 2pm, check-out by 11am. Children stay free with an adult.', 4),
    (3, 'Overview',  'A simple, tidy hotel ten minutes from the airport with a free shuttle.', 1),
    (4, 'Overview',  'A promenade-side hotel on Marine Drive with views across the bay.', 1),
    (4, 'Location',  'Marine Drive, Mumbai 400020, opposite the sea.', 2),
    (11, 'Overview', 'A central Delhi hotel a minute from Connaught Place.', 1),
    (11, 'Amenities','Free Wi-Fi, 24-hour front desk, restaurant.', 2),
    (12, 'Overview', 'Modern suites next to the airport, ideal for early flights.', 1),
    (21, 'Overview', 'A garden hotel on a quiet Indiranagar street.', 1),
    (21, 'Amenities','Free Wi-Fi, garden cafe, co-working corner.', 2),
    (22, 'Overview', 'A business-friendly hotel on MG Road.', 1),
    (23, 'Overview', 'Practical rooms close to the Whitefield tech parks.', 1),
    (31, 'Overview', 'A hilltop hotel in Banjara Hills with city views.', 1),
    (31, 'Policies', 'Check-in from 2pm, check-out by 11am.', 2),
    (32, 'Overview', 'Comfortable rooms in Hitech City for business travel.', 1),
    (41, 'Overview', 'A beachfront hotel on Beach Road.', 1),
    (41, 'Amenities','Free Wi-Fi, sea-view restaurant, 24-hour front desk.', 2),
    (42, 'Overview', 'Simple rooms in T Nagar, close to the shops.', 1),
    (51, 'Overview', 'A beach resort two minutes from Calangute beach.', 1),
    (51, 'Amenities','Pool, beach shack, free Wi-Fi, scooter rental.', 2),
    (51, 'Location', 'Calangute, North Goa 403516.', 3),
    (52, 'Overview', 'A restored Portuguese house in Panjim with a courtyard.', 1),
    (53, 'Overview', 'A relaxed retreat near Palolem beach.', 1)
ON CONFLICT (hotel_id, name) DO UPDATE SET body = EXCLUDED.body, position = EXCLUDED.position;

-- 3. Room types ----------------------------------------------------------------
INSERT INTO room_type (id, hotel_id, name, bed, sleeps, price_per_night_paise) VALUES
    (101, 1, 'Deluxe sea-facing room', '1 king bed',               2,  780000),
    (102, 1, 'Executive room',         '2 twin beds',              3,  960000),
    (103, 1, 'Harbour suite',          '1 king bed and sofa bed',  4, 1420000),
    (104, 1, 'Compact single room',    '1 single bed',             1,  490000),
    (201, 2, 'Standard double room',   '1 queen bed',              2,  640000),
    (202, 2, 'Family room',            '2 queen beds',             4,  820000),
    (301, 3, 'Standard room',          '2 single beds',            3,  420000),
    (401, 4, 'Premium sea-view room',  '1 king bed',               3,  910000),
    (1101, 11, 'Classic room',         '1 queen bed',              2,  620000),
    (1102, 11, 'Family room',          '2 queen beds',             4,  880000),
    (1201, 12, 'Airport suite',        '1 king bed',               3,  740000),
    (1202, 12, 'Twin room',            '2 twin beds',              2,  590000),
    (2101, 21, 'Garden room',          '1 queen bed',              2,  560000),
    (2102, 21, 'Garden family room',   '2 queen beds',             4,  780000),
    (2201, 22, 'Business room',        '1 king bed',               2,  540000),
    (2301, 23, 'Standard room',        '1 queen bed',              2,  470000),
    (2302, 23, 'Twin room',            '2 twin beds',              3,  520000),
    (3101, 31, 'Deluxe room',          '1 king bed',               2,  610000),
    (3102, 31, 'Family room',          '2 queen beds',             4,  830000),
    (3201, 32, 'Standard room',        '1 queen bed',              2,  480000),
    (4101, 41, 'Sea-view room',        '1 king bed',               2,  570000),
    (4102, 41, 'Family sea-view room', '2 queen beds',             4,  790000),
    (4201, 42, 'Comfort room',         '1 queen bed',              2,  420000),
    (5101, 51, 'Beach cottage',        '1 king bed',               2,  690000),
    (5102, 51, 'Pool-side family room','2 queen beds',             4,  940000),
    (5201, 52, 'Heritage room',        '1 queen bed',              2,  580000),
    (5301, 53, 'Retreat hut',          '1 double bed',             2,  450000),
    (5302, 53, 'Retreat family hut',   '2 double beds',            4,  650000)
ON CONFLICT (id) DO UPDATE SET
    hotel_id = EXCLUDED.hotel_id, name = EXCLUDED.name, bed = EXCLUDED.bed,
    sleeps = EXCLUDED.sleeps, price_per_night_paise = EXCLUDED.price_per_night_paise;

-- 4. Inventory: the next 90 nights. DO NOTHING keeps availability that bookings have used.
--    Harbour suite (103) has zero rooms so it shows as sold out.
INSERT INTO room_inventory (room_type_id, night, available)
SELECT q.room_type_id, d::date, q.qty
FROM (VALUES
    (101, 4), (102, 3), (103, 0), (104, 3),
    (201, 5), (202, 3), (301, 2), (401, 1),
    (1101, 5), (1102, 3), (1201, 3), (1202, 2),
    (2101, 5), (2102, 2), (2201, 6), (2301, 4), (2302, 3),
    (3101, 4), (3102, 2), (3201, 5),
    (4101, 4), (4102, 2), (4201, 5),
    (5101, 5), (5102, 3), (5201, 3), (5301, 4), (5302, 2)
) AS q(room_type_id, qty)
CROSS JOIN generate_series(current_date, current_date + 89, interval '1 day') AS d
ON CONFLICT (room_type_id, night) DO NOTHING;

COMMIT;
