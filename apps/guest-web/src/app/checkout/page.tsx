import { notFound, redirect } from "next/navigation";
import { CheckoutScreen } from "@/components/checkout-screen";
import { ApiError, getHotel } from "@/lib/api";
import { nightsBetween, parseStay, stayQuery, type SearchParams } from "@/lib/stay";

function first(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

export default async function CheckoutPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const query = await searchParams;
  const stay = parseStay(query);
  const hotelId = Number.parseInt(first(query.hotelId), 10);
  const roomIds = first(query.roomIds)
    .split(",")
    .map((part) => Number.parseInt(part, 10))
    .filter((n) => Number.isInteger(n));
  if (!stay || !Number.isInteger(hotelId)) redirect("/");

  const hotel = await getHotel(hotelId, stay).catch((error) => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });

  const nights = nightsBetween(stay.checkIn, stay.checkOut);
  // Display only: the server prices the stay again when the order and the booking are created.
  const chosen = hotel.rooms
    .filter((room) => roomIds.includes(room.id) && room.status === "AVAILABLE")
    .map((room) => ({ id: room.id, name: room.name, totalPaise: room.pricePerNightPaise * nights }));
  if (chosen.length === 0) redirect(`/hotels/${hotel.id}?${stayQuery(stay)}`);

  return <CheckoutScreen hotel={{ id: hotel.id, name: hotel.name }} stay={stay} nights={nights} rooms={chosen} />;
}
