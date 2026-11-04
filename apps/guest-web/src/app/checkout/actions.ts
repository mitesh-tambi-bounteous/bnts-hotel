"use server";

import { cookies } from "next/headers";
import { placeBooking, startPayment, type BookingOutcome, type CheckoutInput, type PaymentStart } from "@/lib/checkout";
import { bookingAccessCookie } from "@/lib/booking-access";
import type { OrderRequest } from "@/lib/api";

export async function createPaymentOrder(order: OrderRequest): Promise<PaymentStart> {
  return startPayment(order);
}

export async function submitCheckout(input: CheckoutInput): Promise<BookingOutcome> {
  const outcome = await placeBooking(input);
  if (outcome.ok) {
    // The API only shows a booking to whoever holds its email; keep it out of the URL.
    const { name, options } = bookingAccessCookie(outcome.reference);
    (await cookies()).set(name, input.email.trim(), options);
  }
  return outcome;
}
