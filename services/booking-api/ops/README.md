# Catalogue loader (MOCK data)

`load_catalogue.sql` is a one-off script an operator runs by hand after Flyway V1 has been applied. It is not part of Flyway and the app never runs it.

```sh
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f services/booking-api/ops/load_catalogue.sql
```

**The data in it is mock.** Mumbai is copied from the approved prototype; the other five cities (Delhi, Bengaluru, Hyderabad, Chennai, Goa) are invented. Replace it with real hotels, rooms, prices and inventory before go-live.

| Section | What it needs |
| --- | --- |
| `hotel` | fixed `id`, `brand`, `city`, `name`, `summary`. Upserted by id. |
| `hotel_tab` | one row per tab a hotel should show (`Overview`, `Amenities`, `Location`, `Policies`, ...). A hotel with no row for a tab never shows it. Upserted by `(hotel_id, name)`. |
| `room_type` | fixed `id`, `hotel_id`, `name`, `bed`, `sleeps`, `price_per_night_paise` (rupees x 100, taxes included). Upserted by id. |
| `room_inventory` | rooms available per room type for the next 90 nights from today. Inserted with `ON CONFLICT DO NOTHING`. |

Safe to re-run: a re-run adds newly in-range nights and updates descriptions and prices, but never resets availability that bookings have already used. The script contains no `DELETE`: hotels that must disappear are not handled here.
