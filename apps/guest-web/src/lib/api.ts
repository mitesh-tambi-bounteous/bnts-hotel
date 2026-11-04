import { stayQuery, type Stay } from "@/lib/stay";

export type HotelSummary = { id: number; name: string; summary: string; fromPricePerNightPaise: number };

export type RoomStatus = "AVAILABLE" | "SOLD_OUT" | "UNSUITABLE";

export type RoomView = {
  id: number;
  name: string;
  bed: string;
  sleeps: number;
  pricePerNightPaise: number;
  status: RoomStatus;
  reason: string | null;
};

export type HotelDetail = {
  id: number;
  name: string;
  city: string;
  summary: string;
  tabs: { name: string; body: string }[];
  rooms: RoomView[];
};

export type OrderRequest = {
  hotelId: number;
  checkIn: string;
  checkOut: string;
  guests: number;
  roomTypeIds: number[];
};

export type PaymentOrder = { orderId: string; amountPaise: number; keyId: string };

export type BookingRequest = OrderRequest & {
  name: string;
  email: string;
  phone?: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
};

export type BookingResult = { reference: string; accountCreated: boolean };

export type BookingView = {
  reference: string;
  hotelName: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  rooms: { name: string; pricePerNightPaise: number; totalPaise: number }[];
  totalPaise: number;
  email: string;
  guestName: string;
  accountCreated: boolean;
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors?: Record<string, string>,
    readonly roomTypeId?: number,
  ) {
    super(message);
  }
}

function apiBase(): string {
  return process.env.BOOKING_API_URL ?? "http://localhost:8080";
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiBase()}${path}`, { cache: "no-store", ...init });
  } catch {
    throw new ApiError(0, "UNREACHABLE", "The booking service can't be reached");
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      body.code ?? "ERROR",
      body.message ?? "Something went wrong",
      body.fieldErrors,
      body.roomTypeId,
    );
  }
  return (await response.json()) as T;
}

function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

export function searchHotels(stay: Stay): Promise<HotelSummary[]> {
  return request(`/hotels?${stayQuery(stay)}`);
}

export function getHotel(id: number, stay: Stay): Promise<HotelDetail> {
  return request(`/hotels/${id}?${stayQuery(stay)}`);
}

export function createOrder(order: OrderRequest): Promise<PaymentOrder> {
  return post("/payments/orders", order);
}

export function createBooking(booking: BookingRequest): Promise<BookingResult> {
  return post("/bookings", booking);
}

/** The API shows a booking only together with the email it was made with. */
export function getBooking(reference: string, email: string): Promise<BookingView> {
  return request(`/bookings/${encodeURIComponent(reference)}?email=${encodeURIComponent(email)}`);
}
