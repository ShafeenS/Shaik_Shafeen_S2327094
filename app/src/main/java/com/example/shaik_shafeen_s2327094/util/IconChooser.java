package com.example.shaik_shafeen_s2327094.util;

import com.example.shaik_shafeen_s2327094.model.Situation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decides which icon should be used for a situation based on its type and description.
 */
public final class IconChooser {

    public enum IconType {
        INCIDENT, QUEUE, ACCIDENT, OBSTRUCTION, WEATHER,
        ROADWORKS, CLOSED, LANE, TRAFFIC_LIGHTS, DIVERSION
    }

    // Used to recognise different ways of writing traffic lights.
    private static final Pattern TTL = Pattern.compile("\\bttls?\\b|traffic lights?");

    // Prevents this class from being created because it only contains static methods.
    private IconChooser() { }

    public static IconType choose(Situation s) {
        // Combines the type information and comment to make checking keywords easier.
        String type = (s.getSubType() + " " + s.getRecordType()).toLowerCase(Locale.UK);
        String text = s.getComment().toLowerCase(Locale.UK);

        // Checks roadworks separately because their icons are mainly based on the comment.
        if (s.getKind() == Situation.Kind.ROADWORKS) {
            if (text.contains("obstruction")) return IconType.OBSTRUCTION;
            if (TTL.matcher(text).find()) return IconType.TRAFFIC_LIGHTS;
            if (text.contains("lane")) return IconType.LANE;
            if (text.contains("closure") || text.contains("closed")) return IconType.CLOSED;
            if (text.contains("diversion")) return IconType.DIVERSION;
            return IconType.ROADWORKS;
        }

        // Checks the incident type for keywords to decide which icon is most suitable.
        if (type.contains("accident")) return IconType.ACCIDENT;
        if (type.contains("closed")) return IconType.CLOSED;
        if (type.contains("lane")
                || (type.contains("queu") && s.getImpact().getLanesRestricted() > 0)) {
            return IconType.LANE;
        }
        if (type.contains("queu") || type.contains("abnormaltraffic")
                || type.contains("slow") || type.contains("stationary")) {
            return IconType.QUEUE;
        }
        if (type.contains("obstruction") || type.contains("animal")
                || type.contains("debris") || type.contains("vehicle")) {
            return IconType.OBSTRUCTION;
        }
        if (type.contains("weather") || type.contains("environment") || type.contains("flood")
                || type.contains("snow") || type.contains("fog") || type.contains("wind")) {
            return IconType.WEATHER;
        }

        // Uses the general incident icon if none of the keywords match.
        return IconType.INCIDENT;
    }
}