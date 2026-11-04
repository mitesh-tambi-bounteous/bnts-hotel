package com.bounteous.hotel.bookingworker.email;

import java.text.NumberFormat;
import java.util.Locale;

/** Builds the plain-text booking confirmation. Amounts are integer paise, shown as rupees. */
public class ConfirmationEmailTemplate {

    public EmailMessage render(String to, ConfirmationPayload p) {
        var body = new StringBuilder();
        body.append("Hi ").append(p.firstName()).append(",\n\n")
                .append("Your booking is confirmed.\n\n")
                .append("Booking reference: ").append(p.reference()).append('\n')
                .append("Hotel: ").append(p.hotelName()).append('\n')
                .append("Check-in: ").append(p.checkIn()).append('\n')
                .append("Check-out: ").append(p.checkOut()).append('\n')
                .append("Nights: ").append(p.nights()).append("\n\nRooms:\n");
        for (var room : p.rooms()) {
            body.append("  - ").append(room.name()).append(": ").append(rupees(room.amountPaise())).append('\n');
        }
        body.append("\nTotal paid: ").append(rupees(p.totalPaise())).append("\n\nWe look forward to welcoming you.\n");
        return new EmailMessage(to, "Booking confirmed " + p.reference() + " - " + p.hotelName(), body.toString());
    }

    static String rupees(long paise) {
        var format = NumberFormat.getIntegerInstance(new Locale("en", "IN"));
        long whole = paise / 100;
        long rest = paise % 100;
        return "Rs " + format.format(whole) + (rest == 0 ? "" : String.format(".%02d", rest));
    }
}
