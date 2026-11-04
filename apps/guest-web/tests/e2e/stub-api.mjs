// A stand-in for the Booking API used only by the Playwright suite, so the browser flow runs
// without Postgres or Razorpay. Next's server-side fetches can't be intercepted by Playwright,
// so the stub is a real HTTP server. Magic emails pick the outcome: taken@ and fail@.
import { createServer } from "node:http";

const port = Number(process.env.STUB_API_PORT ?? 4100);

const ROOMS = [
  { id: 101, name: "Deluxe sea-facing room", bed: "1 king bed", sleeps: 2, pricePerNightPaise: 780000, status: "AVAILABLE", reason: null },
  { id: 102, name: "Executive room", bed: "2 twin beds", sleeps: 3, pricePerNightPaise: 960000, status: "AVAILABLE", reason: null },
  { id: 103, name: "Harbour suite", bed: "1 king bed and sofa bed", sleeps: 4, pricePerNightPaise: 1420000, status: "SOLD_OUT", reason: "No rooms left for your dates." },
  {
    id: 104,
    name: "Compact single room",
    bed: "1 single bed",
    sleeps: 1,
    pricePerNightPaise: 490000,
    status: "UNSUITABLE",
    reason: "Doesn't suit your search: sleeps 1, and you're searching 4 guests in 2 rooms.",
  },
];

let takenRoom = null;
let lastBooking = null;

function json(res, status, body) {
  res.writeHead(status, { "content-type": "application/json" });
  res.end(JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = "";
    req.on("data", (chunk) => (data += chunk));
    req.on("end", () => resolve(data ? JSON.parse(data) : {}));
  });
}

function nights(checkIn, checkOut) {
  return Math.round((Date.parse(checkOut) - Date.parse(checkIn)) / 86400000);
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${port}`);

  if (req.method === "POST" && url.pathname === "/__reset") {
    takenRoom = null;
    lastBooking = null;
    return json(res, 200, {});
  }

  if (req.method === "GET" && url.pathname === "/hotels") {
    return json(res, 200, [
      { id: 1, name: "Harbour View Mumbai", summary: "Colaba, sea-facing rooms", fromPricePerNightPaise: 780000 },
      { id: 2, name: "Bandra Court", summary: "Bandra West, near the sea link", fromPricePerNightPaise: 640000 },
    ]);
  }

  if (req.method === "GET" && url.pathname === "/hotels/1") {
    const rooms = ROOMS.map((room) =>
      room.id === takenRoom ? { ...room, status: "SOLD_OUT", reason: "No rooms left for your dates." } : room,
    );
    return json(res, 200, {
      id: 1,
      name: "Harbour View Mumbai",
      city: "Mumbai",
      summary: "Colaba, sea-facing rooms",
      tabs: [
        { name: "Overview", body: "A restored seafront hotel in Colaba." },
        { name: "Amenities", body: "Free Wi-Fi, rooftop pool, 24-hour front desk." },
        { name: "Location", body: "Colaba Causeway, Mumbai 400005." },
      ],
      rooms,
    });
  }

  if (req.method === "POST" && url.pathname === "/payments/orders") {
    const body = await readBody(req);
    const gone = body.roomTypeIds.find((id) => id === takenRoom);
    if (gone) return json(res, 409, { code: "ROOM_UNAVAILABLE", message: "Room unavailable", roomTypeId: gone });
    const total = body.roomTypeIds.reduce(
      (sum, id) => sum + ROOMS.find((room) => room.id === id).pricePerNightPaise * nights(body.checkIn, body.checkOut),
      0,
    );
    return json(res, 200, { orderId: "order_stub_1", amountPaise: total, keyId: "" });
  }

  if (req.method === "POST" && url.pathname === "/bookings") {
    const body = await readBody(req);
    if (body.email === "taken@example.com") {
      takenRoom = 102;
      return json(res, 409, { code: "ROOM_UNAVAILABLE", message: "Room unavailable", roomTypeId: 102 });
    }
    if (body.email === "fail@example.com") {
      return json(res, 502, { code: "BOOKING_FAILED", message: "Booking could not be saved" });
    }
    const n = nights(body.checkIn, body.checkOut);
    const rooms = body.roomTypeIds.map((id) => {
      const room = ROOMS.find((r) => r.id === id);
      return { name: room.name, pricePerNightPaise: room.pricePerNightPaise, totalPaise: room.pricePerNightPaise * n };
    });
    lastBooking = {
      reference: "HB-7K2M9Q",
      hotelName: "Harbour View Mumbai",
      checkIn: body.checkIn,
      checkOut: body.checkOut,
      nights: n,
      rooms,
      totalPaise: rooms.reduce((sum, r) => sum + r.totalPaise, 0),
      email: body.email.toLowerCase(),
      guestName: body.name,
      accountCreated: body.email.toLowerCase() !== "priya@example.com",
    };
    return json(res, 201, { reference: lastBooking.reference, accountCreated: lastBooking.accountCreated });
  }

  if (req.method === "GET" && url.pathname === "/bookings/HB-7K2M9Q" && lastBooking) {
    return json(res, 200, lastBooking);
  }

  return json(res, 404, { code: "NOT_FOUND", message: "Not found" });
});

server.listen(port, () => console.log(`stub booking api on ${port}`));
