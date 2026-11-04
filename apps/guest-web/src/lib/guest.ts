export type GuestDetails = { name: string; email: string; phone: string };

/** Name and email are required; the phone number is optional. Messages match the API's. */
export function validateGuest(guest: GuestDetails): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!guest.name.trim()) errors.name = "Enter your full name";
  const email = guest.email.trim();
  if (!email) errors.email = "Enter your email address";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "Enter a valid email address";
  return errors;
}
