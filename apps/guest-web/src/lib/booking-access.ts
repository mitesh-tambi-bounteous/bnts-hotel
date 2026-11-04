/** The cookie that proves the browser made a booking: it holds the booking email, httpOnly, per reference. */
export function bookingAccessCookie(reference: string) {
  return {
    name: `booking-${reference}`,
    options: {
      httpOnly: true,
      sameSite: "lax" as const,
      secure: process.env.NODE_ENV === "production",
      path: "/booking",
      maxAge: 60 * 60 * 24 * 30,
    },
  };
}
