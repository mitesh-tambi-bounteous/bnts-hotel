import { notFound } from "next/navigation";
import { EmailPreview } from "@/components/email-preview";
import { ApiError, getBooking } from "@/lib/api";

export default async function BookingEmailPage({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = await params;
  const booking = await getBooking(reference).catch((error) => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });
  return <EmailPreview booking={booking} />;
}
