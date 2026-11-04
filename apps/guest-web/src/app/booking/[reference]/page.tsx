import { notFound } from "next/navigation";
import { BookingConfirmation } from "@/components/booking-confirmation";
import { ApiError, getBooking } from "@/lib/api";

export default async function BookingPage({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = await params;
  const booking = await getBooking(reference).catch((error) => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });
  return <BookingConfirmation booking={booking} />;
}
