import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

import { CheckoutForm } from "@/components/checkout-form";
import type { BookingOutcome, PaymentStart } from "@/lib/checkout";
import { PaymentCancelled } from "@/lib/razorpay-checkout";
import { stay } from "./fixtures";

const order: PaymentStart = { ok: true, order: { orderId: "order_1", amountPaise: 3_480_000, keyId: "key_test" } };
const proof = { orderId: "order_1", paymentId: "pay_1", signature: "sig_1" };

function setup(overrides: Partial<React.ComponentProps<typeof CheckoutForm>> = {}) {
  const props = {
    hotel: { id: 1, name: "Harbour View Mumbai" },
    stay,
    nights: 2,
    rooms: [
      { id: 101, name: "Deluxe sea-facing room", totalPaise: 1_560_000 },
      { id: 102, name: "Executive room", totalPaise: 1_920_000 },
    ],
    createOrder: vi.fn().mockResolvedValue(order),
    collectPayment: vi.fn().mockResolvedValue(proof),
    submit: vi.fn().mockResolvedValue({ ok: true, reference: "HB-7K2M9Q", accountCreated: true } as BookingOutcome),
    ...overrides,
  };
  render(<CheckoutForm {...props} />);
  return props;
}

function fill(name: string, email: string, phone = "") {
  fireEvent.change(screen.getByLabelText("Full name"), { target: { value: name } });
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  fireEvent.change(screen.getByLabelText("Phone number (optional)"), { target: { value: phone } });
}

async function pay() {
  await act(async () => {
    fireEvent.click(screen.getByRole("button", { name: /^Pay/ }));
  });
}

beforeEach(() => push.mockReset());

describe("CheckoutForm", () => {
  it("summarises every room and the one total", () => { // AC8
    setup();

    expect(screen.getByText("Deluxe sea-facing room")).toBeTruthy();
    expect(screen.getByText("Executive room")).toBeTruthy();
    expect(screen.getByText("Total, taxes included")).toBeTruthy();
    expect(screen.getAllByText("₹34,800").length).toBeGreaterThan(0);
    expect(screen.getByText("One payment is taken for all your rooms.")).toBeTruthy();
  });

  it("marks the phone number optional", () => { // AC7
    setup();

    expect(screen.getByLabelText("Phone number (optional)")).toBeTruthy();
  });

  it("blocks the booking and explains when name and email are blank", async () => { // AC6
    const props = setup();

    await pay();

    expect(screen.getAllByText("Enter your full name").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Enter your email address").length).toBeGreaterThan(0);
    expect(props.createOrder).not.toHaveBeenCalled();
    expect(props.submit).not.toHaveBeenCalled();
  });

  it("moves focus to the error summary", async () => { // AC6
    setup();

    await pay();

    const summary = screen.getByText("Fix these to continue").closest("[role=alert]") as HTMLElement;
    expect(document.activeElement).toBe(summary);
  });

  it("keeps what the guest typed after a validation error", async () => { // AC6
    setup();
    fill("Asha Rao", "");

    await pay();

    expect((screen.getByLabelText("Full name") as HTMLInputElement).value).toBe("Asha Rao");
  });

  it("books with one payment for all rooms and goes to the confirmation", async () => { // AC8, AC10
    const props = setup();
    fill("Asha Rao", "asha@example.com");

    await pay();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/booking/HB-7K2M9Q"));
    expect(props.createOrder).toHaveBeenCalledTimes(1);
    expect(props.createOrder).toHaveBeenCalledWith({
      hotelId: 1,
      checkIn: "2026-12-12",
      checkOut: "2026-12-14",
      guests: 4,
      roomTypeIds: [101, 102],
    });
    expect(props.collectPayment).toHaveBeenCalledTimes(1);
    expect(props.submit).toHaveBeenCalledTimes(1);
    expect(vi.mocked(props.submit).mock.calls[0][0]).toMatchObject({ roomTypeIds: [101, 102], email: "asha@example.com", proof });
  });

  it("submits without a phone number", async () => { // AC7
    const props = setup();
    fill("Asha Rao", "asha@example.com");

    await pay();

    await waitFor(() => expect(props.submit).toHaveBeenCalled());
    expect(vi.mocked(props.submit).mock.calls[0][0].phone).toBe("");
  });

  it("disables the button while the payment is pending so it can't be sent twice", async () => { // AC8
    let release: (value: BookingOutcome) => void = () => {};
    const props = setup({ submit: vi.fn().mockReturnValue(new Promise<BookingOutcome>((r) => (release = r))) });
    fill("Asha Rao", "asha@example.com");

    await pay();

    const button = screen.getByRole("button", { name: /Paying/ }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fireEvent.click(button);
    expect(props.createOrder).toHaveBeenCalledTimes(1);
    await act(async () => release({ ok: true, reference: "HB-AAAAAA", accountCreated: false }));
  });

  it("sends the guest back to the room list when a room was taken, with nothing charged", async () => { // AC16
    setup({
      submit: vi.fn().mockResolvedValue({ ok: false, code: "ROOM_UNAVAILABLE", message: "gone", roomTypeId: 102 }),
    });
    fill("Asha Rao", "asha@example.com");

    await pay();

    await waitFor(() => expect(push).toHaveBeenCalled());
    const url = new URL(push.mock.calls[0][0], "http://x");
    expect(url.pathname).toBe("/hotels/1");
    expect(url.searchParams.get("taken")).toBe("102");
    expect(url.searchParams.get("roomIds")).toBe("101");
  });

  it("sends the guest back when the room is already gone at the order step", async () => { // AC16
    const props = setup({
      createOrder: vi.fn().mockResolvedValue({ ok: false, code: "ROOM_UNAVAILABLE", message: "gone", roomTypeId: 102 }),
    });
    fill("Asha Rao", "asha@example.com");

    await pay();

    await waitFor(() => expect(push).toHaveBeenCalled());
    expect(props.collectPayment).not.toHaveBeenCalled();
  });

  it("tells the guest the payment was reversed when the booking couldn't be saved", async () => { // AC19
    setup({ submit: vi.fn().mockResolvedValue({ ok: false, code: "BOOKING_FAILED", message: "x" }) });
    fill("Asha Rao", "asha@example.com");

    await pay();

    expect(await screen.findByText("We couldn't complete your booking")).toBeTruthy();
    expect(screen.getByText(/the payment was reversed in full/)).toBeTruthy();
    expect(push).not.toHaveBeenCalled();
    expect((screen.getByRole("button", { name: /^Pay/ }) as HTMLButtonElement).disabled).toBe(false);
  });

  it("says nothing was charged when the guest closes the Razorpay window", async () => {
    const props = setup({ collectPayment: vi.fn().mockRejectedValue(new PaymentCancelled()) });
    fill("Asha Rao", "asha@example.com");

    await pay();

    expect(await screen.findByText("Payment cancelled")).toBeTruthy();
    expect(screen.getByText(/haven't been charged/)).toBeTruthy();
    expect(props.submit).not.toHaveBeenCalled();
  });

  it("shows a payment problem when the server can't confirm the payment", async () => {
    setup({ submit: vi.fn().mockResolvedValue({ ok: false, code: "PAYMENT_FAILED", message: "x" }) });
    fill("Asha Rao", "asha@example.com");

    await pay();

    expect(await screen.findByText("We couldn't confirm your payment")).toBeTruthy();
  });
});
