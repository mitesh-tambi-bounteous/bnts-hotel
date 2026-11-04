import { BookingConfirmation } from "@/components/booking-confirmation";
import { loadBooking } from "@/lib/load-booking";

export default async function BookingPage({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = await params;
  return <BookingConfirmation booking={await loadBooking(reference)} />;
}
