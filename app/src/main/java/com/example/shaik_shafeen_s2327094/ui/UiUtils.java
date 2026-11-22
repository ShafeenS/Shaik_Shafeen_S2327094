package com.example.shaik_shafeen_s2327094.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.model.Situation;
import com.example.shaik_shafeen_s2327094.util.IconChooser;

/**
 * Picks the icons and colours used to display a traffic situation.
 */
public final class UiUtils {

    // Prevents this class from being created as it only contains static methods.
    private UiUtils() { }

    /**
     * Returns the correct icon for a situation using IconChooser.
     */
    public static int iconFor(Situation s) {
        switch (IconChooser.choose(s)) {
            case QUEUE:          return R.drawable.ic_queue;
            case ACCIDENT:       return R.drawable.ic_accident;
            case OBSTRUCTION:    return R.drawable.ic_obstruction;
            case WEATHER:        return R.drawable.ic_weather;
            case ROADWORKS:      return R.drawable.ic_roadworks;
            case CLOSED:         return R.drawable.ic_closed;
            case LANE:           return R.drawable.ic_lane;
            case TRAFFIC_LIGHTS: return R.drawable.ic_traffic_lights;
            case DIVERSION:      return R.drawable.ic_diversion;
            default:             return R.drawable.ic_incident;
        }
    }

    /**
     * Gets the colour for the situation. Incidents use one colour,
     * while roadworks use a colour based on the delay level.
     */
    public static int stripColor(Context c, Situation s) {
        if (s.getKind() == Situation.Kind.INCIDENT) {
            return ContextCompat.getColor(c, R.color.traffic_incident);
        }
        return delayColor(c, s.getDelayLevel());
    }

    // Returns a colour depending on how long the traffic delay is.
    public static int delayColor(Context c, int level) {
        switch (level) {
            case 1:  return ContextCompat.getColor(c, R.color.traffic_delay_short);
            case 2:  return ContextCompat.getColor(c, R.color.traffic_delay_long);
            case 3:  return ContextCompat.getColor(c, R.color.traffic_delay_very_long);
            default: return ContextCompat.getColor(c, R.color.traffic_delay_none);
        }
    }
}