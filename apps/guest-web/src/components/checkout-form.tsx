"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Icon } from "@/components/icon";
import type { OrderRequest, PaymentOrder } from "@/lib/api";
import type { BookingOutcome, CheckoutInput, Failure, PaymentProof, PaymentStart } from "@/lib/checkout";
import { validateGuest } from "@/lib/guest";
import { formatPaise } from "@/lib/money";
import { PaymentCancelled } from "@/lib/razorpay-checkout";
import { stayQuery, type Stay } from "@/lib/stay";

type Props = {
  hotel: { id: number; name: string };
  stay: Stay;
  nights: number;
  rooms: { id: number; name: string; totalPaise: number }[];
  createOrder: (order: OrderRequest) => Promise<PaymentStart>;
  collectPayment: (
    order: PaymentOrder,
    prefill: { name: string; email: string; phone: string },
  ) => Promise<PaymentProof>;
  submit: (input: CheckoutInput) => Promise<BookingOutcome>;
};

type Banner = "booking-failed" | "cancelled" | "payment-failed" | "error";

const BANNERS: Record<Banner, { title: string; body: string }> = {
  "booking-failed": {
    title: "We couldn't complete your booking",
    body: "Your card was charged and then the payment was reversed in full, so you haven't paid anything. Try booking again, or choose different rooms.",
  },
  cancelled: {
    title: "Payment cancelled",
    body: "You haven't been charged. Pay again when you're ready.",
  },
  "payment-failed": {
    title: "We couldn't confirm your payment",
    body: "We couldn't verify the payment, so no booking was made. Try again.",
  },
  error: {
    title: "Something went wrong",
    body: "Try again in a moment.",
  },
};

export function CheckoutForm({ hotel, stay, nights, rooms, createOrder, collectPayment, submit }: Props) {
  const router = useRouter();
  const [values, setValues] = useState({ name: "", email: "", phone: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [pending, setPending] = useState(false);
  const [banner, setBanner] = useState<Banner | null>(null);
  const summary = useRef<HTMLDivElement>(null);
  const inFlight = useRef(false);

  const errorList = Object.values(errors);
  useEffect(() => {
    if (errorList.length > 0) summary.current?.focus();
  }, [errors]); // eslint-disable-line react-hooks/exhaustive-deps

  const roomTypeIds = rooms.map((room) => room.id);
  const total = rooms.reduce((sum, room) => sum + room.totalPaise, 0);
  const order: OrderRequest = {
    hotelId: hotel.id,
    checkIn: stay.checkIn,
    checkOut: stay.checkOut,
    guests: stay.guests,
    roomTypeIds,
  };

  function backToRooms(takenId?: number) {
    const params = new URLSearchParams(stayQuery(stay));
    const remaining = roomTypeIds.filter((id) => id !== takenId);
    if (takenId !== undefined) params.set("taken", String(takenId));
    if (remaining.length > 0) params.set("roomIds", remaining.join(","));
    router.push(`/hotels/${hotel.id}?${params}`);
  }

  /** Returns true when the guest has been sent elsewhere and the form should stay locked. */
  function handleFailure(failure: Failure): boolean {
    if (failure.code === "ROOM_UNAVAILABLE") {
      backToRooms(failure.roomTypeId);
      return true;
    }
    if (failure.code === "VALIDATION" && failure.fieldErrors) setErrors(failure.fieldErrors);
    else if (failure.code === "BOOKING_FAILED") setBanner("booking-failed");
    else if (failure.code === "PAYMENT_FAILED") setBanner("payment-failed");
    else setBanner("error");
    return false;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) return;
    setBanner(null);
    const found = validateGuest(values);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    inFlight.current = true;
    setPending(true);
    let leaving = false;
    try {
      const started = await createOrder(order);
      if (!started.ok) {
        leaving = handleFailure(started);
        return;
      }
      let proof: PaymentProof;
      try {
        proof = await collectPayment(started.order, values);
      } catch (error) {
        setBanner(error instanceof PaymentCancelled ? "cancelled" : "error");
        return;
      }
      const result = await submit({ ...order, ...values, proof });
      if (result.ok) {
        leaving = true;
        router.push(`/booking/${result.reference}`);
        return;
      }
      leaving = handleFailure(result);
    } catch {
      setBanner("error");
    } finally {
      if (!leaving) {
        inFlight.current = false;
        setPending(false);
      }
    }
  }

  function field(name: "name" | "email" | "phone") {
    return {
      value: values[name],
      onChange: (event: { target: { value: string } }) => setValues((v) => ({ ...v, [name]: event.target.value })),
    };
  }

  const shown = banner ? BANNERS[banner] : null;

  return (
    <form className="stack-lg" onSubmit={onSubmit} noValidate>
      <div>
        <a className="btn btn-ghost" href={`/hotels/${hotel.id}?${stayQuery(stay)}&roomIds=${roomTypeIds.join(",")}`}>
          <Icon name="arrow-left" />
          Back to rooms
        </a>
      </div>
      <h1>Checkout</h1>

      {shown ? (
        <div className="alert alert-danger" role="alert">
          <span className="alert-title">{shown.title}</span>
          <span>{shown.body}</span>
        </div>
      ) : null}

      {errorList.length > 0 ? (
        <div className="alert alert-danger error-summary" role="alert" tabIndex={-1} ref={summary}>
          <span className="alert-title">Fix these to continue</span>
          <ul className="error-list">
            {errorList.map((message) => (
              <li key={message}>{message}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <section className="card stack" aria-labelledby="summary-heading">
        <h2 id="summary-heading">Your booking</h2>
        <div className="stack-sm">
          {rooms.map((room) => (
            <div className="line" key={room.id}>
              <span>{room.name}</span>
              <span>{formatPaise(room.totalPaise)}</span>
            </div>
          ))}
        </div>
        <hr className="divider" />
        <div className="line text-bold">
          <span>Total, taxes included</span>
          <span>{formatPaise(total)}</span>
        </div>
        <p className="text-sm text-muted">{`${hotel.name}, ${nights} night${nights === 1 ? "" : "s"}`}</p>
      </section>

      <section className="stack" aria-labelledby="guest-heading">
        <h2 id="guest-heading">Guest details</h2>
        <div className="field">
          <label htmlFor="name">Full name</label>
          <input
            className={`input${errors.name ? " is-invalid" : ""}`}
            id="name"
            autoComplete="name"
            aria-invalid={Boolean(errors.name)}
            aria-describedby="e-name"
            {...field("name")}
          />
          {errors.name ? (
            <span className="field-error" id="e-name">
              {errors.name}
            </span>
          ) : null}
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input
            className={`input${errors.email ? " is-invalid" : ""}`}
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            aria-invalid={Boolean(errors.email)}
            aria-describedby="h-email e-email"
            {...field("email")}
          />
          <span className="field-hint" id="h-email">
            We send your confirmation here.
          </span>
          {errors.email ? (
            <span className="field-error" id="e-email">
              {errors.email}
            </span>
          ) : null}
        </div>
        <div className="field">
          <label htmlFor="phone">Phone number (optional)</label>
          <input className="input" id="phone" type="tel" autoComplete="tel" aria-describedby="h-phone" {...field("phone")} />
          <span className="field-hint" id="h-phone">
            Only used if the hotel needs to reach you.
          </span>
        </div>
      </section>

      <section className="stack" aria-labelledby="pay-heading">
        <h2 id="pay-heading">Pay securely</h2>
        <p className="text-muted">
          You&apos;ll pay in Razorpay&apos;s secure window by card, UPI or net banking. Card details never reach us.
        </p>
        <p className="text-sm text-muted">One payment is taken for all your rooms.</p>
      </section>

      <button className="btn btn-primary btn-lg" type="submit" disabled={pending}>
        {pending ? (
          <>
            <span className="spinner" aria-hidden="true" />
            Paying…
          </>
        ) : (
          `Pay ${formatPaise(total)}`
        )}
      </button>
    </form>
  );
}
