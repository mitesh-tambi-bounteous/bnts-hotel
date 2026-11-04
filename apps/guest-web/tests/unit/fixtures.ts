import type { BookingView, RoomView } from "@/lib/api";
import type { Stay } from "@/lib/stay";

export const stay: Stay = { city: "Mumbai", checkIn: "2026-12-12", checkOut: "2026-12-14", guests: 4, rooms: 2 };

export const rooms: RoomView[] = [
  { id: 101, name: "Deluxe sea-facing room", bed: "1 king bed", sleeps: 2, pricePerNightPaise: 780_000, status: "AVAILABLE", reason: null },
  { id: 102, name: "Executive room", bed: "2 twin beds", sleeps: 3, pricePerNightPaise: 960_000, status: "AVAILABLE", reason: null },
  {
    id: 103,
    name: "Harbour suite",
    bed: "1 king bed and sofa bed",
    sleeps: 4,
    pricePerNightPaise: 1_420_000,
    status: "SOLD_OUT",
    reason: "No rooms left for your dates.",
  },
  {
    id: 104,
    name: "Compact single room",
    bed: "1 single bed",
    sleeps: 1,
    pricePerNightPaise: 490_000,
    status: "UNSUITABLE",
    reason: "Doesn't suit your search: sleeps 1, and you're searching 4 guests in 2 rooms.",
  },
];

export const booking: BookingView = {
  reference: "HB-7K2M9Q",
  hotelName: "Harbour View Mumbai",
  checkIn: "2026-12-12",
  checkOut: "2026-12-14",
  nights: 2,
  rooms: [
    { name: "Deluxe sea-facing room", pricePerNightPaise: 780_000, totalPaise: 1_560_000 },
    { name: "Executive room", pricePerNightPaise: 960_000, totalPaise: 1_920_000 },
  ],
  totalPaise: 3_480_000,
  email: "asha@example.com",
  guestName: "Asha Rao",
  accountCreated: true,
};
