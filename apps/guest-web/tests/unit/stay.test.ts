import { describe, expect, it } from "vitest";
import { nightsBetween, parseStay, stayQuery, validateStay } from "@/lib/stay";

const base = { city: "Mumbai", checkIn: "2026-12-12", checkOut: "2026-12-14", guests: "4", rooms: "2" };

describe("parseStay", () => {
  it("reads the search params", () => {
    expect(parseStay(base)).toEqual({
      city: "Mumbai",
      checkIn: "2026-12-12",
      checkOut: "2026-12-14",
      guests: 4,
      rooms: 2,
    });
  });

  it("returns null when the stay is incomplete", () => {
    expect(parseStay({ ...base, city: undefined })).toBeNull();
    expect(parseStay({ ...base, checkIn: "" })).toBeNull();
    expect(parseStay({ ...base, guests: "abc" })).toBeNull();
  });

  it("takes the first value of a repeated param", () => {
    expect(parseStay({ ...base, city: ["Goa", "Delhi"] })?.city).toBe("Goa");
  });
});

describe("nightsBetween", () => {
  it("counts nights", () => {
    expect(nightsBetween("2026-12-12", "2026-12-14")).toBe(2);
    expect(nightsBetween("2026-12-30", "2027-01-02")).toBe(3);
  });
});

describe("validateStay", () => {
  const stay = parseStay(base)!;

  it("accepts a good stay", () => {
    expect(validateStay(stay, "2026-12-01")).toEqual({});
  });

  it("requires check-out after check-in", () => {
    expect(validateStay({ ...stay, checkOut: "2026-12-12" }, "2026-12-01").checkOut).toBeDefined();
  });

  it("refuses a check-in in the past", () => {
    expect(validateStay(stay, "2026-12-20").checkIn).toBeDefined();
  });

  it("needs at least one guest per room", () => {
    expect(validateStay({ ...stay, guests: 1, rooms: 2 }, "2026-12-01").guests).toBeDefined();
  });
});

describe("stayQuery", () => {
  it("round-trips through parseStay", () => {
    const stay = parseStay(base)!;
    const params = Object.fromEntries(new URLSearchParams(stayQuery(stay)));
    expect(parseStay(params)).toEqual(stay);
  });
});
