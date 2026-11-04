import { Icon } from "@/components/icon";
import type { BookingView } from "@/lib/api";
import { formatDate, nightsLabel } from "@/lib/dates";
import { formatPaise } from "@/lib/money";

/** The confirmation email as the guest receives it (the worker sends the same facts by SES). */
export function EmailPreview({ booking }: { booking: BookingView }) {
  const firstName = booking.guestName.trim().split(/\s+/)[0];
  return (
    <div className="stack-lg">
      <div>
        <a className="btn btn-ghost" href={`/booking/${booking.reference}`}>
          <Icon name="arrow-left" />
          Back to confirmation
        </a>
      </div>
      <div className="email">
        <div className="email-head stack-sm">
          <div className="text-sm text-muted">{`To: ${booking.email}`}</div>
          <div className="text-bold">{`Your stay at ${booking.hotelName} is booked`}</div>
        </div>
        <div className="email-body stack">
          <p>{`Hi ${firstName}, your rooms are ready for you.`}</p>
          <div>
            <div className="text-sm text-muted">Booking reference</div>
            <div className="text-lg text-bold">{booking.reference}</div>
          </div>
          <div>
            <div className="text-sm text-muted">Dates</div>
            <div>{`Check-in ${formatDate(booking.checkIn)}, check-out ${formatDate(booking.checkOut)} (${nightsLabel(booking.nights)})`}</div>
          </div>
          <div>
            <div className="text-sm text-muted">Rooms</div>
            <div className="stack-sm">
              {booking.rooms.map((room, index) => (
                <div className="line" key={`${room.name}-${index}`}>
                  <span>{room.name}</span>
                  <span>{formatPaise(room.totalPaise)}</span>
                </div>
              ))}
            </div>
          </div>
          <hr className="divider" />
          <div className="line text-bold">
            <span>Total paid, taxes included</span>
            <span>{formatPaise(booking.totalPaise)}</span>
          </div>
          <p className="text-sm text-muted">Show this reference at the front desk when you arrive.</p>
        </div>
      </div>
    </div>
  );
}
