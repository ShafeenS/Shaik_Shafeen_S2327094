package com.example.shaik_shafeen_s2327094.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small helper methods for making raw DATEX II text easier to read.
 */
public final class Format {

    // Sets the date and time format using UK formatting.
    private static final DateTimeFormatter OUT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK);

    // Prevents objects of this class from being created.
    private Format() { }

    /**
     * Changes camelCase text into normal readable text.
     * For example, "queuingTraffic" becomes "Queuing traffic".
     */
    public static String pretty(String camel) {
        if (camel == null || camel.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        // Goes through each character and adds a space before capital letters.
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Converts an ISO date and time into a more readable format
     * using the phone's current time zone.
     */
    public static String time(String iso) {
        if (iso == null || iso.isEmpty()) return "";

        try {
            return OffsetDateTime.parse(iso)
                    .atZoneSameInstant(ZoneId.systemDefault()).format(OUT);
        } catch (Exception e) {
            // If the date cannot be converted, return the original text.
            return iso;
        }
    }

    /**
     * Makes direction values easier to read, such as "eastBound" becoming "Eastbound".
     */
    public static String direction(String d) {
        if (d == null || d.isEmpty()) return "";
        return pretty(d).replace("bound", "bound").replace(" bound", "bound");
    }

    // Returns a text label depending on the delay level.
    public static String delayLabel(int level) {
        switch (level) {
            case 1: return "Short delays";
            case 2: return "Long delays";
            case 3: return "Very long delays";
            default: return "No delays reported";
        }
    }
}