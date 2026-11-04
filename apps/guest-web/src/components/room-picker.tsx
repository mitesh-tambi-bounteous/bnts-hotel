"use client";

import { useState } from "react";
import { Icon } from "@/components/icon";
import type { RoomView } from "@/lib/api";
import { nightsLabel, roomsLabel } from "@/lib/dates";
import { formatPaise } from "@/lib/money";
import { selectionTotalPaise, toggleRoom } from "@/lib/selection";
import type { Stay } from "@/lib/stay";

type Props = {
  hotelId: number;
  stay: Stay;
  nights: number;
  rooms: RoomView[];
  initialSelected?: number[];
};

function StatusBadge({ room, picked }: { room: RoomView; picked: boolean }) {
  if (room.status === "SOLD_OUT") {
    return (
      <span className="badge badge-danger">
        <Icon name="x" />
        Sold out
      </span>
    );
  }
  if (room.status === "UNSUITABLE") {
    return (
      <span className="badge badge-warning">
        <Icon name="info" />
        Doesn&apos;t suit your search
      </span>
    );
  }
  return picked ? (
    <span className="badge badge-success">
      <Icon name="check" />
      Added to booking
    </span>
  ) : null;
}

export function RoomPicker({ hotelId, stay, nights, rooms, initialSelected = [] }: Props) {
  const bookable = new Set(rooms.filter((room) => room.status === "AVAILABLE").map((room) => room.id));
  const [selected, setSelected] = useState(() => initialSelected.filter((id) => bookable.has(id)));

  const chosen = rooms.filter((room) => selected.includes(room.id));
  const total = selectionTotalPaise(rooms, selected, nights);
  const checkout = new URLSearchParams({
    hotelId: String(hotelId),
    city: stay.city,
    checkIn: stay.checkIn,
    checkOut: stay.checkOut,
    guests: String(stay.guests),
    rooms: String(stay.rooms),
    roomIds: selected.join(","),
  });

  return (
    <div className="stack-lg">
      <div className="stack" role="list">
        {rooms.map((room) => {
          const picked = selected.includes(room.id);
          const off = room.status !== "AVAILABLE";
          const note = room.status === "SOLD_OUT" ? (room.reason ?? "No rooms left for your dates.") : room.reason;
          return (
            <div
              key={room.id}
              role="listitem"
              className={`card room-card stack-sm${picked ? " is-selected" : ""}${off ? " is-off" : ""}`}
            >
              <div className="row-between">
                <h3>{room.name}</h3>
                <StatusBadge room={room} picked={picked} />
              </div>
              <p className="text-sm text-muted">{`${room.bed}, sleeps ${room.sleeps}`}</p>
              <div className="row-between">
                <span>
                  <span className="text-bold">{formatPaise(room.pricePerNightPaise)}</span>{" "}
                  <span className="text-sm text-muted">per night, taxes included</span>
                </span>
                {off ? null : (
                  <button
                    className={`btn ${picked ? "btn-secondary" : "btn-primary"}`}
                    type="button"
                    onClick={() => setSelected((current) => toggleRoom(current, room.id))}
                  >
                    {picked ? "Remove room" : "Select room"}
                  </button>
                )}
              </div>
              {off && note ? <p className="text-sm text-muted">{note}</p> : null}
            </div>
          );
        })}
      </div>

      <div className="tray stack" aria-live="polite">
        <h2>Your booking</h2>
        {chosen.length === 0 ? (
          <>
            <p className="text-muted">Select a room to start your booking.</p>
            <button className="btn btn-primary" type="button" disabled>
              Continue to checkout
            </button>
          </>
        ) : (
          <>
            {chosen.map((room) => (
              <div className="line" key={room.id}>
                <span>{room.name}</span>
                <span>{formatPaise(room.pricePerNightPaise * nights)}</span>
              </div>
            ))}
            <div className="line text-bold">
              <span>{`${roomsLabel(chosen.length)}, ${nightsLabel(nights)}, taxes included`}</span>
              <span>{formatPaise(total)}</span>
            </div>
            <a className="btn btn-primary" href={`/checkout?${checkout}`}>
              Continue to checkout
            </a>
          </>
        )}
      </div>
    </div>
  );
}
