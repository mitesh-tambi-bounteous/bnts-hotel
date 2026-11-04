import type { BookingView } from "@/lib/api";
import { formatDate, nightsLabel } from "@/lib/dates";
import { formatPaise } from "@/lib/money";

function paymentNote(rooms: number): string {
  if (rooms === 1) return "One card payment for your room.";
  if (rooms === 2) return "One card payment for both rooms.";
  return `One card payment for all ${rooms} rooms.`;
}

export function BookingConfirmation({ booking }: { booking: BookingView }) {
  const accountLine = booking.accountCreated
    ? `We've created an account for ${booking.email}.`
    : `This booking is linked to your existing account for ${booking.email}.`;

  return (
    <div className="stack-lg">
      <div className="alert alert-success" role="status">
        <span className="alert-title">Your rooms are booked</span>
        <span>{`We've sent your confirmation to ${booking.email}.`}</span>
      </div>

      <div className="stack-sm">
        <p className="text-sm text-muted">Booking reference</p>
        <div className="reference">{booking.reference}</div>
      </div>

      <section className="card stack" aria-labelledby="booking-heading">
        <h2 id="booking-heading">{booking.hotelName}</h2>
        <p className="text-muted">{`${formatDate(booking.checkIn)} to ${formatDate(booking.checkOut)}, ${nightsLabel(booking.nights)}`}</p>
        <div className="stack-sm">
          {booking.rooms.map((room, index) => (
            <div className="line" key={`${room.name}-${index}`}>
              <span>{room.name}</span>
              <span>{formatPaise(room.totalPaise)}</span>
            </div>
          ))}
        </div>
        <hr className="divider" />
        <div className="line text-bold">
          <span>Total paid, taxes included</span>
          <span>{formatPaise(booking.totalPaise)}</span>
        </div>
        <p className="text-sm text-muted">{paymentNote(booking.rooms.length)}</p>
      </section>

      <div className="alert alert-info">{accountLine}</div>

      <div className="stack-sm">
        <a className="btn btn-primary" href={`/booking/${booking.reference}/email`}>
          View confirmation email
        </a>
        <a className="btn btn-ghost" href="/">
          Search more hotels
        </a>
      </div>
    </div>
  );
}
