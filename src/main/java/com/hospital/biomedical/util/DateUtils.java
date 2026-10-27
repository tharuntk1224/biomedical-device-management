package com.hospital.biomedical.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date and time parsing and formatting utilities.
 */
public final class DateUtils {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {}

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "N/A";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "N/A";
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text) || "N/A".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(text.trim()); // Fallback to ISO-8601 standard
        }
    }

    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text) || "N/A".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text.trim()); // Fallback to ISO-8601 standard
        }
    }
}
