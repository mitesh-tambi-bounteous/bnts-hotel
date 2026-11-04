import { CITIES, type Stay } from "@/lib/stay";

const GUEST_OPTIONS = [1, 2, 3, 4, 6, 9];
const ROOM_OPTIONS = [1, 2, 3];

function FieldError({ id, message }: { id: string; message?: string }) {
  return message ? (
    <span className="field-error" id={id}>
      {message}
    </span>
  ) : null;
}

/** A plain GET form: the stay lives in the URL, so a search can be shared, reloaded and refreshed. */
export function SearchForm({ stay, errors = {} }: { stay: Stay; errors?: Record<string, string> }) {
  return (
    <form className="search-bar" action="/" method="get" data-testid="search-form">
      <div className="field">
        <label htmlFor="city">City</label>
        <select className="select" id="city" name="city" defaultValue={stay.city}>
          {CITIES.map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="checkIn">Check-in</label>
        <input
          className={`input${errors.checkIn ? " is-invalid" : ""}`}
          id="checkIn"
          name="checkIn"
          type="date"
          defaultValue={stay.checkIn}
          aria-describedby="e-checkIn"
          aria-invalid={Boolean(errors.checkIn)}
        />
        <FieldError id="e-checkIn" message={errors.checkIn} />
      </div>
      <div className="field">
        <label htmlFor="checkOut">Check-out</label>
        <input
          className={`input${errors.checkOut ? " is-invalid" : ""}`}
          id="checkOut"
          name="checkOut"
          type="date"
          defaultValue={stay.checkOut}
          aria-describedby="e-checkOut"
          aria-invalid={Boolean(errors.checkOut)}
        />
        <FieldError id="e-checkOut" message={errors.checkOut} />
      </div>
      <div className="field">
        <label htmlFor="guests">Guests</label>
        <select className="select" id="guests" name="guests" defaultValue={String(stay.guests)}>
          {GUEST_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n === 1 ? "1 guest" : `${n} guests`}
            </option>
          ))}
        </select>
        <FieldError id="e-guests" message={errors.guests} />
      </div>
      <div className="field">
        <label htmlFor="rooms">Rooms</label>
        <select className="select" id="rooms" name="rooms" defaultValue={String(stay.rooms)}>
          {ROOM_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n === 1 ? "1 room" : `${n} rooms`}
            </option>
          ))}
        </select>
      </div>
      <button className="btn btn-primary" type="submit">
        Search hotels
      </button>
    </form>
  );
}
