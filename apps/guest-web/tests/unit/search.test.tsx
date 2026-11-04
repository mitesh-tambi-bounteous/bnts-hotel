import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { HotelCard } from "@/components/hotel-card";
import { SearchForm } from "@/components/search-form";
import { SearchResults } from "@/components/search-results";
import { stay } from "./fixtures";

const hotels = [
  { id: 1, name: "Harbour View Mumbai", summary: "Colaba, sea-facing rooms", fromPricePerNightPaise: 780_000 },
  { id: 2, name: "Bandra Court", summary: "Bandra West, near the sea link", fromPricePerNightPaise: 640_000 },
];

describe("SearchForm", () => {
  it("asks for city, dates, guests and rooms and searches by GET", () => { // AC1
    const { container } = render(<SearchForm stay={stay} />);

    for (const label of ["City", "Check-in", "Check-out", "Guests", "Rooms"]) {
      expect(screen.getByLabelText(label)).toBeTruthy();
    }
    expect((screen.getByLabelText("City") as HTMLSelectElement).value).toBe("Mumbai");
    expect((screen.getByLabelText("Check-in") as HTMLInputElement).value).toBe("2026-12-12");
    expect((screen.getByLabelText("Guests") as HTMLSelectElement).value).toBe("4");
    expect(screen.getByRole("button", { name: "Search hotels" })).toBeTruthy();
    expect(container.querySelector("form")?.getAttribute("method")).toBe("get");
  });

  it("offers the brand's six cities", () => {
    render(<SearchForm stay={stay} />);

    const options = Array.from((screen.getByLabelText("City") as HTMLSelectElement).options).map((o) => o.value);
    expect(options).toEqual(["Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Chennai", "Goa"]);
  });

  it("shows a validation message next to the field", () => {
    render(<SearchForm stay={stay} errors={{ checkOut: "Check-out must be after check-in" }} />);

    expect(screen.getByText("Check-out must be after check-in")).toBeTruthy();
  });
});

describe("HotelCard", () => {
  it("shows the name, summary and a per-night price with taxes included", () => { // AC2
    render(<HotelCard hotel={hotels[0]} stay={stay} />);

    expect(screen.getByRole("heading", { name: "Harbour View Mumbai" })).toBeTruthy();
    expect(screen.getByText("Colaba, sea-facing rooms")).toBeTruthy();
    expect(screen.getByText("Per night, taxes included")).toBeTruthy();
    expect(screen.getByText("₹7,800")).toBeTruthy();
  });

  it("links to the hotel page carrying the stay", () => { // AC3
    render(<HotelCard hotel={hotels[0]} stay={stay} />);

    const href = screen.getByRole("link", { name: "View rooms" }).getAttribute("href")!;
    const url = new URL(href, "http://x");
    expect(url.pathname).toBe("/hotels/1");
    expect(url.searchParams.get("checkIn")).toBe("2026-12-12");
    expect(url.searchParams.get("guests")).toBe("4");
    expect(url.searchParams.get("rooms")).toBe("2");
  });
});

describe("SearchResults", () => {
  it("titles the list with the city and lists every hotel", () => { // AC1
    render(<SearchResults stay={stay} hotels={hotels} />);

    expect(screen.getByRole("heading", { name: "Hotels in Mumbai" })).toBeTruthy();
    expect(screen.getAllByRole("link", { name: "View rooms" })).toHaveLength(2);
  });
});
