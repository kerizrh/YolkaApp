package com.yolka.app.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtils {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "MX"));

    public static String calculateReadableAge(String birthDateStr, Integer fallbackYears) {
        if (birthDateStr != null && !birthDateStr.isBlank()) {
            try {
                LocalDate birth = LocalDate.parse(birthDateStr.trim().substring(0, 10));
                Period period = Period.between(birth, LocalDate.now());
                int years = Math.max(0, period.getYears());
                int months = Math.max(0, period.getMonths());
                if (years > 0 && months > 0) {
                    return years + (years == 1 ? " año " : " años ") + months + (months == 1 ? " mes" : " meses");
                } else if (years > 0) {
                    return years + (years == 1 ? " año" : " años");
                } else if (months > 0) {
                    return "Aprox. " + months + (months == 1 ? " mes" : " meses");
                } else {
                    return "Recién nacido";
                }
            } catch (Exception ignored) {
            }
        }
        int y = fallbackYears != null ? fallbackYears : 1;
        return y + (y == 1 ? " año" : " años");
    }

    public static String formatIsoDate(String isoOrYmd) {
        if (isoOrYmd == null || isoOrYmd.isBlank()) return "Pendiente";
        try {
            String clean = isoOrYmd.trim();
            if (clean.length() >= 10) {
                LocalDate date = LocalDate.parse(clean.substring(0, 10));
                return date.format(DISPLAY_DATE);
            }
        } catch (Exception ignored) {
        }
        return isoOrYmd;
    }

    public static String formatTime12h(String time24h) {
        if (time24h == null || time24h.isBlank()) return "08:00 AM";
        try {
            String[] parts = time24h.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1].substring(0, 2)) : 0;
            String ampm = hour >= 12 ? "PM" : "AM";
            int h12 = hour % 12;
            if (h12 == 0) h12 = 12;
            return String.format(Locale.US, "%02d:%02d %s", h12, min, ampm);
        } catch (Exception e) {
            return time24h;
        }
    }
}
