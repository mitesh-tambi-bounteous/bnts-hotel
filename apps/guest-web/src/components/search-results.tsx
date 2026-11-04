import { HotelCard } from "@/components/hotel-card";
import type { HotelSummary } from "@/lib/api";
import type { Stay } from "@/lib/stay";

export function SearchResults({ stay, hotels }: { stay: Stay; hotels: HotelSummary[] }) {
  return (
    <div className="stack">
      <h2>Hotels in {stay.city}</h2>
      <div className="grid">
        {hotels.map((hotel) => (
          <HotelCard key={hotel.id} hotel={hotel} stay={stay} />
        ))}
      </div>
    </div>
  );
}
