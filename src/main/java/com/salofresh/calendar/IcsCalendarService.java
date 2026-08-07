package com.salofresh.calendar;

import com.salofresh.entity.Appointment;
import com.salofresh.entity.Salon;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Hand-builds RFC 5545 iCalendar content and Google Calendar "quick add" links for a booked
 * appointment. No external calendar library is used; this is plain, spec-compliant string
 * building. Appointment date/time is combined and rendered as a UTC instant (a floating local
 * time interpreted as UTC) for internal consistency between DTSTART/DTEND/DTSTAMP - this is
 * acceptable for a small business-hours booking use case where no VTIMEZONE component is
 * required.
 */
@Service
public class IcsCalendarService {

    private static final DateTimeFormatter ICS_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public String buildIcsForAppointment(Appointment appointment) {
        String dtStart = toUtcIcsDateTime(appointment.getAppointmentDate().atTime(appointment.getStartTime()));
        String dtEnd = toUtcIcsDateTime(appointment.getAppointmentDate().atTime(appointment.getEndTime()));
        String dtStamp = toUtcIcsDateTime(LocalDateTime.now());

        String summary = buildSummary(appointment);
        String location = buildLocation(appointment);
        String description = buildDescription(appointment);
        String uid = "booking-" + appointment.getBookingNumber() + "@salofresh.com";

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR").append("\r\n");
        ics.append("VERSION:2.0").append("\r\n");
        ics.append("PRODID:-//SaloFresh//Appointment Booking//EN").append("\r\n");
        ics.append("CALSCALE:GREGORIAN").append("\r\n");
        ics.append("METHOD:PUBLISH").append("\r\n");
        ics.append("BEGIN:VEVENT").append("\r\n");
        ics.append("UID:").append(uid).append("\r\n");
        ics.append("DTSTAMP:").append(dtStamp).append("\r\n");
        ics.append("DTSTART:").append(dtStart).append("\r\n");
        ics.append("DTEND:").append(dtEnd).append("\r\n");
        ics.append("SUMMARY:").append(escapeText(summary)).append("\r\n");
        ics.append("LOCATION:").append(escapeText(location)).append("\r\n");
        ics.append("DESCRIPTION:").append(escapeText(description)).append("\r\n");
        ics.append("STATUS:CONFIRMED").append("\r\n");
        ics.append("END:VEVENT").append("\r\n");
        ics.append("END:VCALENDAR").append("\r\n");
        return ics.toString();
    }

    public String buildGoogleCalendarLink(Appointment appointment) {
        String dtStart = toUtcIcsDateTime(appointment.getAppointmentDate().atTime(appointment.getStartTime()));
        String dtEnd = toUtcIcsDateTime(appointment.getAppointmentDate().atTime(appointment.getEndTime()));

        return "https://www.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + urlEncode(buildSummary(appointment))
                + "&dates=" + dtStart + "/" + dtEnd
                + "&location=" + urlEncode(buildLocation(appointment))
                + "&details=" + urlEncode(buildDescription(appointment));
    }

    private String toUtcIcsDateTime(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneOffset.UTC).format(ICS_DATE_TIME_FORMAT);
    }

    private String buildSummary(Appointment appointment) {
        Salon salon = appointment.getSalon();
        String salonName = salon != null ? salon.getName() : null;
        return salonName == null || salonName.isBlank() ? "Salon Appointment" : "Salon Appointment - " + salonName;
    }

    private String buildLocation(Appointment appointment) {
        Salon salon = appointment.getSalon();
        if (salon == null) {
            return "";
        }
        StringBuilder location = new StringBuilder(salon.getAddressLine1() == null ? "" : salon.getAddressLine1());
        if (salon.getAddressLine2() != null && !salon.getAddressLine2().isBlank()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(salon.getAddressLine2());
        }
        return location.toString();
    }

    private String buildDescription(Appointment appointment) {
        Salon salon = appointment.getSalon();
        String salonName = salon != null ? salon.getName() : "your salon";
        return "Booking #" + appointment.getBookingNumber() + " at " + salonName;
    }

    private String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
