import { Icon } from "@/components/icon";
import type { HotelSummary } from "@/lib/api";
import { formatPaise } from "@/lib/money";
import { stayQuery, type Stay } from "@/lib/stay";

export function HotelCard({ hotel, stay }: { hotel: HotelSummary; stay: Stay }) {
  return (
    <div className="card stack-sm">
      <div className="hotel-image">
        <Icon name="image" />
      </div>
      <h3>{hotel.name}</h3>
      <p className="text-sm text-muted">{hotel.summary}</p>
      <div className="stat">
        <span className="text-sm text-muted">Per night, taxes included</span>
        <span className="stat-value">{formatPaise(hotel.fromPricePerNightPaise)}</span>
      </div>
      <a className="btn btn-secondary" href={`/hotels/${hotel.id}?${stayQuery(stay)}`}>
        View rooms
      </a>
    </div>
  );
}
