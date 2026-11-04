import { notFound, redirect } from "next/navigation";
import { HotelTabs } from "@/components/hotel-tabs";
import { Icon } from "@/components/icon";
import { RoomPicker } from "@/components/room-picker";
import { ApiError, getHotel } from "@/lib/api";
import { formatDate, nightsLabel, roomsLabel } from "@/lib/dates";
import { nightsBetween, parseStay, stayQuery, type SearchParams } from "@/lib/stay";

function ids(value: string | string[] | undefined): number[] {
  const text = Array.isArray(value) ? value[0] : value;
  return (text ?? "")
    .split(",")
    .map((part) => Number.parseInt(part, 10))
    .filter((n) => Number.isInteger(n));
}

export default async function HotelPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<SearchParams>;
}) {
  const { id } = await params;
  const query = await searchParams;
  const stay = parseStay(query);
  const hotelId = Number.parseInt(id, 10);
  if (!stay) redirect("/");
  if (!Number.isInteger(hotelId)) notFound();

  const hotel = await getHotel(hotelId, stay).catch((error) => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });

  const nights = nightsBetween(stay.checkIn, stay.checkOut);
  const takenId = ids(query.taken)[0];
  const taken = hotel.rooms.find((room) => room.id === takenId);

  return (
    <div className="stack-lg">
      <div>
        <a className="btn btn-ghost" href={`/?${stayQuery(stay)}`}>
          <Icon name="arrow-left" />
          Back to results
        </a>
      </div>

      {taken ? (
        <div className="alert alert-danger" role="alert">
          <span className="alert-title">That room is no longer available</span>
          <span>{`The ${taken.name} was booked by another guest while you were checking out. You haven't been charged. Choose another room below.`}</span>
        </div>
      ) : null}

      <div className="stack-sm">
        <h1>{hotel.name}</h1>
        <p className="text-muted">{`${formatDate(stay.checkIn)} to ${formatDate(stay.checkOut)}, ${nightsLabel(nights)} · ${stay.guests} guest${stay.guests === 1 ? "" : "s"} · ${roomsLabel(stay.rooms)}`}</p>
      </div>

      <HotelTabs tabs={hotel.tabs} />

      <div className="stack">
        <h2>Rooms for your stay</h2>
        <RoomPicker hotelId={hotel.id} stay={stay} nights={nights} rooms={hotel.rooms} initialSelected={ids(query.roomIds)} />
      </div>
    </div>
  );
}
