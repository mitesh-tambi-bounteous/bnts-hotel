-- Local development seed (MOCK data, loaded only under the `local` profile).
-- The prototype's Mumbai hotels, rooms, tabs and the priya@example.com account, with fixed ids.
-- Idempotent: every insert is ON CONFLICT DO NOTHING.
INSERT INTO hotel (id, brand, city, name, summary) VALUES
    (1, 'Hotel Brand', 'Mumbai', 'Harbour View Mumbai',    'Colaba, sea-facing rooms'),
    (2, 'Hotel Brand', 'Mumbai', 'Bandra Court',           'Bandra West, near the sea link'),
    (3, 'Hotel Brand', 'Mumbai', 'Airport Inn Andheri',    'Ten minutes from the airport'),
    (4, 'Hotel Brand', 'Mumbai', 'Marine Drive Residency', 'Marine Drive, promenade views')
ON CONFLICT DO NOTHING;

INSERT INTO hotel_tab (hotel_id, name, body, position) VALUES
    (1, 'Overview',  'A restored seafront hotel in Colaba, a short walk from the Gateway of India. Breakfast is served on the terrace from 7am.', 1),
    (1, 'Amenities', 'Free Wi-Fi, rooftop pool, 24-hour front desk, airport transfers on request.', 2),
    (1, 'Location',  'Colaba Causeway, Mumbai 400005. Ten minutes by taxi from Chhatrapati Shivaji Terminus.', 3),
    (2, 'Overview',  'A calm hotel in Bandra West with a quiet courtyard and a short drive to the sea link.', 1),
    (2, 'Amenities', 'Free Wi-Fi, gym, in-house cafe, laundry service.', 2),
    (2, 'Location',  'Hill Road, Bandra West, Mumbai 400050. Ten minutes from Bandra station.', 3),
    (2, 'Policies',  'Check-in from 2pm, check-out by 11am. Children stay free with an adult.', 4),
    (3, 'Overview',  'A simple, tidy hotel ten minutes from the airport with a free shuttle.', 1),
    (4, 'Overview',  'A promenade-side hotel on Marine Drive with views across the bay.', 1)
ON CONFLICT DO NOTHING;

INSERT INTO room_type (id, hotel_id, name, bed, sleeps, price_per_night_paise) VALUES
    (101, 1, 'Deluxe sea-facing room', '1 king bed',              2,  780000),
    (102, 1, 'Executive room',         '2 twin beds',             3,  960000),
    (103, 1, 'Harbour suite',          '1 king bed and sofa bed', 4, 1420000),
    (104, 1, 'Compact single room',    '1 single bed',            1,  490000),
    (201, 2, 'Standard double room',   '1 queen bed',             2,  640000),
    (202, 2, 'Family room',            '2 queen beds',            4,  820000),
    (301, 3, 'Standard room',          '2 single beds',           3,  420000),
    (401, 4, 'Premium sea-view room',  '1 king bed',              3,  910000)
ON CONFLICT DO NOTHING;

-- Harbour suite (103) has no rooms left, so it shows as sold out.
INSERT INTO room_inventory (room_type_id, night, available)
SELECT q.room_type_id, d::date, q.qty
FROM (VALUES (101, 4), (102, 3), (103, 0), (104, 3), (201, 5), (202, 3), (301, 2), (401, 1))
    AS q(room_type_id, qty)
CROSS JOIN generate_series(current_date, current_date + 89, interval '1 day') AS d
ON CONFLICT DO NOTHING;

INSERT INTO guest_account (email) VALUES ('priya@example.com') ON CONFLICT DO NOTHING;
