import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, createBooking, getBooking, getHotel, searchHotels } from "@/lib/api";

function respond(status: number, body: unknown) {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } }));
}

afterEach(() => vi.unstubAllGlobals());

const stay = { city: "Mumbai", checkIn: "2026-12-12", checkOut: "2026-12-14", guests: 4, rooms: 2 };

describe("api client", () => {
  it("searches with the stay as query params, uncached", async () => {
    const fetchMock = respond(200, [{ id: 1, name: "Harbour View Mumbai", summary: "s", fromPricePerNightPaise: 780_000 }]);
    vi.stubGlobal("fetch", fetchMock);

    const hotels = await searchHotels(stay);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/hotels?");
    expect(String(url)).toContain("city=Mumbai");
    expect(String(url)).toContain("rooms=2");
    expect(init.cache).toBe("no-store");
    expect(hotels[0].name).toBe("Harbour View Mumbai");
  });

  it("loads a hotel for a stay", async () => {
    const fetchMock = respond(200, { id: 1, name: "H", city: "Mumbai", summary: "", tabs: [], rooms: [] });
    vi.stubGlobal("fetch", fetchMock);

    await getHotel(1, stay);

    expect(String(fetchMock.mock.calls[0][0])).toContain("/hotels/1?");
  });

  it("asks for a booking with the email it was made with", async () => {
    const fetchMock = respond(200, { reference: "HB-7K2M9Q" });
    vi.stubGlobal("fetch", fetchMock);

    await getBooking("HB-7K2M9Q", "a+b@example.com");

    expect(String(fetchMock.mock.calls[0][0])).toContain("/bookings/HB-7K2M9Q?email=a%2Bb%40example.com");
  });

  it("turns an error body into an ApiError carrying its code", async () => {
    vi.stubGlobal("fetch", respond(409, { code: "ROOM_UNAVAILABLE", message: "gone", roomTypeId: 102 }));

    const error = await createBooking({} as never).catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, code: "ROOM_UNAVAILABLE", roomTypeId: 102 });
  });

  it("reports an unreachable API as a generic error", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("connect refused")));

    const error = await createBooking({} as never).catch((e) => e);

    expect(error).toMatchObject({ status: 0, code: "UNREACHABLE" });
  });
});
