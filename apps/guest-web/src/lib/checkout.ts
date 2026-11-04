import { ApiError, createBooking, createOrder, type OrderRequest, type PaymentOrder } from "@/lib/api";
import { validateGuest } from "@/lib/guest";

export type PaymentProof = { orderId: string; paymentId: string; signature: string };

export type CheckoutInput = OrderRequest & {
  name: string;
  email: string;
  phone: string;
  proof: PaymentProof;
};

export type FailureCode = "VALIDATION" | "ROOM_UNAVAILABLE" | "BOOKING_FAILED" | "PAYMENT_FAILED" | "ERROR";

export type Failure = {
  ok: false;
  code: FailureCode;
  message: string;
  fieldErrors?: Record<string, string>;
  roomTypeId?: number;
};

export type BookingOutcome = { ok: true; reference: string; accountCreated: boolean } | Failure;
export type PaymentStart = { ok: true; order: PaymentOrder } | Failure;

const KNOWN: FailureCode[] = ["VALIDATION", "ROOM_UNAVAILABLE", "BOOKING_FAILED", "PAYMENT_FAILED"];

function failure(error: unknown): Failure {
  if (error instanceof ApiError) {
    const code = KNOWN.includes(error.code as FailureCode) ? (error.code as FailureCode) : "ERROR";
    return { ok: false, code, message: error.message, fieldErrors: error.fieldErrors, roomTypeId: error.roomTypeId };
  }
  return { ok: false, code: "ERROR", message: "Something went wrong" };
}

/** Creates the Razorpay order. The server prices the stay; no amount is ever sent from here. */
export async function startPayment(order: OrderRequest): Promise<PaymentStart> {
  try {
    return { ok: true, order: await createOrder(order) };
  } catch (error) {
    return failure(error);
  }
}

/** One booking for every selected room, after the guest's details check out. */
export async function placeBooking(input: CheckoutInput): Promise<BookingOutcome> {
  const fieldErrors = validateGuest(input);
  if (Object.keys(fieldErrors).length > 0) {
    return { ok: false, code: "VALIDATION", message: "Fix these to continue", fieldErrors };
  }
  try {
    const result = await createBooking({
      hotelId: input.hotelId,
      checkIn: input.checkIn,
      checkOut: input.checkOut,
      guests: input.guests,
      roomTypeIds: input.roomTypeIds,
      name: input.name.trim(),
      email: input.email.trim(),
      phone: input.phone.trim() || undefined,
      razorpayOrderId: input.proof.orderId,
      razorpayPaymentId: input.proof.paymentId,
      razorpaySignature: input.proof.signature,
    });
    return { ok: true, reference: result.reference, accountCreated: result.accountCreated };
  } catch (error) {
    return failure(error);
  }
}
