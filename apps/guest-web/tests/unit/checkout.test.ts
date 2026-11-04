import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return { ...actual, createBooking: vi.fn(), createOrder: vi.fn() };
});

import { ApiError, createBooking, createOrder } from "@/lib/api";
import { placeBooking, startPayment, type CheckoutInput } from "@/lib/checkout";

const input: CheckoutInput = {
  hotelId: 1,
  checkIn: "2026-12-12",
  checkOut: "2026-12-14",
  guests: 4,
  roomTypeIds: [101, 102],
  name: "Asha Rao",
  email: "asha@example.com",
  phone: "",
  proof: { orderId: "order_fake_1", paymentId: "pay_fake_1", signature: "fake" },
};

beforeEach(() => vi.resetAllMocks());

describe("placeBooking", () => {
  it("returns the one reference for every room", async () => {
    vi.mocked(createBooking).mockResolvedValue({ reference: "HB-7K2M9Q", accountCreated: true });

    const result = await placeBooking(input);

    expect(result).toEqual({ ok: true, reference: "HB-7K2M9Q", accountCreated: true });
    expect(createBooking).toHaveBeenCalledTimes(1);
    expect(vi.mocked(createBooking).mock.calls[0][0]).toMatchObject({
      roomTypeIds: [101, 102],
      razorpayPaymentId: "pay_fake_1",
    });
  });

  it("does not call the API when the name or email is blank", async () => { // AC6
    const result = await placeBooking({ ...input, name: " ", email: "" });

    expect(result).toMatchObject({ ok: false, code: "VALIDATION" });
    expect(createBooking).not.toHaveBeenCalled();
  });

  it("sends no phone when none was entered", async () => { // AC7
    vi.mocked(createBooking).mockResolvedValue({ reference: "HB-AAAAAA", accountCreated: false });

    await placeBooking(input);

    expect(vi.mocked(createBooking).mock.calls[0][0].phone).toBeUndefined();
  });

  it.each([
    [409, "ROOM_UNAVAILABLE"],
    [502, "BOOKING_FAILED"],
    [402, "PAYMENT_FAILED"],
  ])("maps a %i %s response to a failure the page can show", async (status, code) => { // AC16, AC19
    vi.mocked(createBooking).mockRejectedValue(new ApiError(status, code, "message", undefined, 102));

    const result = await placeBooking(input);

    expect(result).toMatchObject({ ok: false, code });
  });

  it("carries the taken room id for ROOM_UNAVAILABLE", async () => { // AC16
    vi.mocked(createBooking).mockRejectedValue(new ApiError(409, "ROOM_UNAVAILABLE", "gone", undefined, 102));

    expect(await placeBooking(input)).toMatchObject({ roomTypeId: 102 });
  });

  it("falls back to a generic error for anything unexpected", async () => {
    vi.mocked(createBooking).mockRejectedValue(new ApiError(0, "UNREACHABLE", "down"));

    expect(await placeBooking(input)).toMatchObject({ ok: false, code: "ERROR" });
  });
});

describe("startPayment", () => {
  it("asks the server for an order, never sending an amount", async () => {
    vi.mocked(createOrder).mockResolvedValue({ orderId: "order_1", amountPaise: 3_480_000, keyId: "key_test" });

    const result = await startPayment({ hotelId: 1, checkIn: "2026-12-12", checkOut: "2026-12-14", guests: 4, roomTypeIds: [101, 102] });

    expect(result).toEqual({ ok: true, order: { orderId: "order_1", amountPaise: 3_480_000, keyId: "key_test" } });
    expect(JSON.stringify(vi.mocked(createOrder).mock.calls[0][0])).not.toContain("amount");
  });

  it("reports a sold-out room before any payment is taken", async () => { // AC16
    vi.mocked(createOrder).mockRejectedValue(new ApiError(409, "ROOM_UNAVAILABLE", "gone", undefined, 103));

    expect(await startPayment({ hotelId: 1, checkIn: "2026-12-12", checkOut: "2026-12-14", guests: 4, roomTypeIds: [103] })).toMatchObject({
      ok: false,
      code: "ROOM_UNAVAILABLE",
      roomTypeId: 103,
    });
  });
});
