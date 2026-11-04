import { describe, expect, it } from "vitest";
import { selectionTotalPaise, toggleRoom } from "@/lib/selection";

const rooms = [
  { id: 101, name: "Deluxe sea-facing room", pricePerNightPaise: 780_000 },
  { id: 102, name: "Executive room", pricePerNightPaise: 960_000 },
];

describe("toggleRoom", () => {
  it("adds a room then removes it", () => {
    expect(toggleRoom([], 101)).toEqual([101]);
    expect(toggleRoom([101, 102], 101)).toEqual([102]);
  });

  it("keeps the order rooms were chosen in", () => {
    expect(toggleRoom([102], 101)).toEqual([102, 101]);
  });
});

describe("selectionTotalPaise", () => {
  it("is price per night times nights for every chosen room", () => {
    expect(selectionTotalPaise(rooms, [101, 102], 2)).toBe(3_480_000);
  });

  it("is zero with nothing chosen", () => {
    expect(selectionTotalPaise(rooms, [], 2)).toBe(0);
  });
});
