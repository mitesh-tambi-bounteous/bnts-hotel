import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BookingConfirmation } from "@/components/booking-confirmation";
import { EmailPreview } from "@/components/email-preview";
import { formatDate } from "@/lib/dates";
import { booking } from "./fixtures";

describe("formatDate", () => {
  it("shows day, short month and year", () => {
    expect(formatDate("2026-12-12")).toBe("12 Dec 2026");
  });
});

describe("BookingConfirmation", () => {
  it("shows one booking reference for all rooms", () => { // AC10, AC11
    render(<BookingConfirmation booking={booking} />);

    expect(screen.getAllByText("HB-7K2M9Q")).toHaveLength(1);
    expect(screen.getByText("Booking reference")).toBeTruthy();
  });

  it("lists the hotel, dates, each room and the total paid", () => { // AC12
    render(<BookingConfirmation booking={booking} />);

    expect(screen.getByRole("heading", { name: "Harbour View Mumbai" })).toBeTruthy();
    expect(screen.getByText("12 Dec 2026 to 14 Dec 2026, 2 nights")).toBeTruthy();
    expect(screen.getByText("Deluxe sea-facing room")).toBeTruthy();
    expect(screen.getByText("Executive room")).toBeTruthy();
    expect(screen.getByText("₹34,800")).toBeTruthy();
    expect(screen.getByText("One card payment for both rooms.")).toBeTruthy();
  });

  it("says where the confirmation email went", () => { // AC13
    render(<BookingConfirmation booking={booking} />);

    expect(screen.getByText("We've sent your confirmation to asha@example.com.")).toBeTruthy();
  });

  it("says an account was created for a new guest", () => { // AC14
    render(<BookingConfirmation booking={booking} />);

    expect(screen.getByText(/We've created an account for asha@example.com/)).toBeTruthy();
  });

  it("says the booking is linked to the existing account", () => { // AC15
    render(<BookingConfirmation booking={{ ...booking, accountCreated: false }} />);

    expect(screen.getByText(/linked to your existing account for asha@example.com/)).toBeTruthy();
    expect(screen.queryByText(/created an account/)).toBeNull();
  });

  it("links on to the email and back to search", () => {
    render(<BookingConfirmation booking={booking} />);

    expect(screen.getByRole("link", { name: "View confirmation email" }).getAttribute("href")).toBe("/booking/HB-7K2M9Q/email");
    expect(screen.getByRole("link", { name: "Search more hotels" }).getAttribute("href")).toBe("/");
  });
});

describe("EmailPreview", () => {
  it("shows what the guest receives: reference, dates, rooms and total", () => { // AC13
    render(<EmailPreview booking={booking} />);

    expect(screen.getByText("To: asha@example.com")).toBeTruthy();
    expect(screen.getByText("Your stay at Harbour View Mumbai is booked")).toBeTruthy();
    expect(screen.getByText(/Hi Asha/)).toBeTruthy();
    expect(screen.getByText("HB-7K2M9Q")).toBeTruthy();
    expect(screen.getByText("Check-in 12 Dec 2026, check-out 14 Dec 2026 (2 nights)")).toBeTruthy();
    expect(screen.getByText("₹34,800")).toBeTruthy();
  });
});
