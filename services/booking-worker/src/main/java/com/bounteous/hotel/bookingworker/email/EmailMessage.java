package com.bounteous.hotel.bookingworker.email;

public record EmailMessage(String to, String subject, String text) {}
