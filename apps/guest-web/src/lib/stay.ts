export type Stay = {
  city: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  rooms: number;
};

export type SearchParams = Record<string, string | string[] | undefined>;

export const CITIES = ["Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Chennai", "Goa"] as const;

function first(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

/** Reads a stay from URL search params; null when any part is missing or not a number. */
export function parseStay(params: SearchParams): Stay | null {
  const city = first(params.city).trim();
  const checkIn = first(params.checkIn);
  const checkOut = first(params.checkOut);
  const guests = Number.parseInt(first(params.guests), 10);
  const rooms = Number.parseInt(first(params.rooms), 10);
  if (!city || !checkIn || !checkOut || !Number.isInteger(guests) || !Number.isInteger(rooms)) return null;
  return { city, checkIn, checkOut, guests, rooms };
}

export function stayQuery(stay: Stay): string {
  return new URLSearchParams({
    city: stay.city,
    checkIn: stay.checkIn,
    checkOut: stay.checkOut,
    guests: String(stay.guests),
    rooms: String(stay.rooms),
  }).toString();
}

const DAY_MS = 86_400_000;

export function nightsBetween(checkIn: string, checkOut: string): number {
  return Math.round((Date.parse(checkOut) - Date.parse(checkIn)) / DAY_MS);
}

/** Field-level messages; an empty object means the stay can be searched. `today` is an ISO date. */
export function validateStay(stay: Stay, today: string): Record<string, string> {
  const errors: Record<string, string> = {};
  if (stay.checkIn < today) errors.checkIn = "Check-in can't be in the past";
  if (nightsBetween(stay.checkIn, stay.checkOut) < 1) errors.checkOut = "Check-out must be after check-in";
  if (stay.rooms < 1) errors.rooms = "Choose at least one room";
  if (stay.guests < Math.max(1, stay.rooms)) errors.guests = "You need at least one guest per room";
  return errors;
}
