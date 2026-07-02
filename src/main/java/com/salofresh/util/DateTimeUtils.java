package com.salofresh.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtils() {
    }

    public static boolean isWithinRange(LocalTime target, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !target.isBefore(start) && target.isBefore(end);
        }
        return !target.isBefore(start) || target.isBefore(end);
    }

    public static boolean rangesOverlap(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }
}
