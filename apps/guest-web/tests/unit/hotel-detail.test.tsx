import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { HotelTabs } from "@/components/hotel-tabs";
import { RoomPicker } from "@/components/room-picker";
import { rooms, stay } from "./fixtures";

const tabs = [
  { name: "Overview", body: "A restored seafront hotel in Colaba." },
  { name: "Amenities", body: "Free Wi-Fi, rooftop pool." },
  { name: "Location", body: "Colaba Causeway, Mumbai 400005." },
];

describe("HotelTabs", () => {
  it("shows only the tabs the catalogue defines", () => { // AC4
    render(<HotelTabs tabs={tabs} />);

    expect(screen.getAllByRole("tab").map((t) => t.textContent)).toEqual(["Overview", "Amenities", "Location"]);
    expect(screen.queryByRole("tab", { name: "Policies" })).toBeNull();
  });

  it("shows the first tab's content and switches on click", () => {
    render(<HotelTabs tabs={tabs} />);
    expect(screen.getByText("A restored seafront hotel in Colaba.")).toBeTruthy();

    fireEvent.click(screen.getByRole("tab", { name: "Amenities" }));

    expect(screen.getByRole("tab", { name: "Amenities" }).getAttribute("aria-selected")).toBe("true");
    expect(screen.getByText("Free Wi-Fi, rooftop pool.")).toBeTruthy();
    expect(screen.queryByText("A restored seafront hotel in Colaba.")).toBeNull();
  });

  it("renders no tab bar for a hotel with no catalogue content", () => {
    render(<HotelTabs tabs={[]} />);

    expect(screen.queryByRole("tablist")).toBeNull();
  });
});

function picker(initialSelected: number[] = []) {
  return render(<RoomPicker hotelId={1} stay={stay} nights={2} rooms={rooms} initialSelected={initialSelected} />);
}

describe("RoomPicker", () => {
  it("lists each room with bed, sleeps and a per-night price with taxes included", () => { // AC3
    picker();

    const card = screen.getByRole("heading", { name: "Deluxe sea-facing room" }).closest(".card") as HTMLElement;
    expect(within(card).getByText("1 king bed, sleeps 2")).toBeTruthy();
    expect(within(card).getByText("₹7,800")).toBeTruthy();
    expect(within(card).getByText("per night, taxes included")).toBeTruthy();
  });

  it("offers Select room only on rooms that can be booked", () => { // AC21, AC22
    picker();

    expect(screen.getAllByRole("button", { name: "Select room" })).toHaveLength(2);
    for (const name of ["Harbour suite", "Compact single room"]) {
      const card = screen.getByRole("heading", { name }).closest(".card") as HTMLElement;
      expect(within(card).queryByRole("button")).toBeNull();
    }
  });

  it("marks a sold-out room with the reason", () => { // AC21
    picker();

    const card = screen.getByRole("heading", { name: "Harbour suite" }).closest(".card") as HTMLElement;
    expect(within(card).getByText("Sold out")).toBeTruthy();
    expect(within(card).getByText("No rooms left for your dates.")).toBeTruthy();
  });

  it("marks an unsuitable room with the reason from the search", () => { // AC22, AC23
    picker();

    const card = screen.getByRole("heading", { name: "Compact single room" }).closest(".card") as HTMLElement;
    expect(within(card).getByText("Doesn't suit your search")).toBeTruthy();
    expect(
      within(card).getByText("Doesn't suit your search: sleeps 1, and you're searching 4 guests in 2 rooms."),
    ).toBeTruthy();
  });

  it("starts with Continue disabled and a prompt", () => { // AC5
    picker();

    expect(screen.getByText("Select a room to start your booking.")).toBeTruthy();
    const continueButton = screen.getByRole("button", { name: "Continue to checkout" }) as HTMLButtonElement;
    expect(continueButton.disabled).toBe(true);
  });

  it("adds two rooms to one booking with the total for the stay", () => { // AC5, AC8
    picker();

    const select = screen.getAllByRole("button", { name: "Select room" });
    fireEvent.click(select[0]);
    fireEvent.click(screen.getAllByRole("button", { name: "Select room" })[0]);

    const tray = screen.getByText("Your booking").closest(".tray") as HTMLElement;
    expect(within(tray).getByText("Deluxe sea-facing room")).toBeTruthy();
    expect(within(tray).getByText("Executive room")).toBeTruthy();
    expect(within(tray).getByText("2 rooms, 2 nights, taxes included")).toBeTruthy();
    expect(within(tray).getByText("₹34,800")).toBeTruthy();
  });

  it("continues to checkout with the chosen rooms and the stay", () => {
    picker([101, 102]);

    const href = screen.getByRole("link", { name: "Continue to checkout" }).getAttribute("href")!;
    const url = new URL(href, "http://x");
    expect(url.pathname).toBe("/checkout");
    expect(url.searchParams.get("hotelId")).toBe("1");
    expect(url.searchParams.get("roomIds")).toBe("101,102");
    expect(url.searchParams.get("checkOut")).toBe("2026-12-14");
  });

  it("removes a chosen room", () => {
    picker([101]);

    fireEvent.click(screen.getByRole("button", { name: "Remove room" }));

    expect(screen.getByText("Select a room to start your booking.")).toBeTruthy();
  });

  it("never preselects a room that can't be booked", () => { // AC16 (room taken: guest returns here)
    picker([102, 103]);

    const tray = screen.getByText("Your booking").closest(".tray") as HTMLElement;
    expect(within(tray).getByText("1 room, 2 nights, taxes included")).toBeTruthy();
  });
});
