import { EmailPreview } from "@/components/email-preview";
import { loadBooking } from "@/lib/load-booking";

export default async function BookingEmailPage({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = await params;
  return <EmailPreview booking={await loadBooking(reference)} />;
}
