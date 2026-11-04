"use server";

import { placeBooking, startPayment, type BookingOutcome, type CheckoutInput, type PaymentStart } from "@/lib/checkout";
import type { OrderRequest } from "@/lib/api";

export async function createPaymentOrder(order: OrderRequest): Promise<PaymentStart> {
  return startPayment(order);
}

export async function submitCheckout(input: CheckoutInput): Promise<BookingOutcome> {
  return placeBooking(input);
}
