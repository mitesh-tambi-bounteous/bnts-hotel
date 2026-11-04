import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import { ApiError, getBooking, type BookingView } from "@/lib/api";
import { bookingAccessCookie } from "@/lib/booking-access";

/** Loads a booking for the browser that made it; anyone else gets the same not-found page. */
export async function loadBooking(reference: string): Promise<BookingView> {
  const email = (await cookies()).get(bookingAccessCookie(reference).name)?.value;
  if (!email) notFound();
  return getBooking(reference, email).catch((error) => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });
}
