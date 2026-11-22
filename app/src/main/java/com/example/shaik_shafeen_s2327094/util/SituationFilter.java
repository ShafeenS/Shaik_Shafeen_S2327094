package com.example.shaik_shafeen_s2327094.util;

import com.example.shaik_shafeen_s2327094.model.Situation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Contains the filtering and searching methods used by the search screens.
 * Keeping this separate from the UI also makes it easier to test.
 */
public final class SituationFilter {

    public static final int ALL = 0, INCIDENTS = 1, ROADWORKS = 2;

    // Prevents objects of this class from being created.
    private SituationFilter() { }

    /**
     * Finds situations matching a road name, depending on the selected filter.
     */
    public static List<Situation> byRoad(List<Situation> incidents, List<Situation> roadworks,
                                         String query, int filter) {
        List<Situation> out = new ArrayList<>();

        // Returns an empty list if the search field has not been filled in.
        if (query == null || query.trim().isEmpty()) return out;

        // Adds matching incidents or roadworks depending on the selected filter.
        if (filter != ROADWORKS) addMatches(out, incidents, query);
        if (filter != INCIDENTS) addMatches(out, roadworks, query);

        return out;
    }

    // Checks each situation in the list and adds it if the road matches the search.
    private static void addMatches(List<Situation> out, List<Situation> src, String q) {
        if (src == null) return;
        for (Situation s : src) if (s.matchesRoad(q)) out.add(s);
    }

    /**
     * Finds situations within the selected radius and sorts them by distance,
     * with the nearest situations appearing first. Their distances are also saved in kmOut.
     */
    public static List<Situation> nearby(List<Situation> all, double lat, double lon,
                                         double radiusKm, Map<String, Double> kmOut) {
        kmOut.clear();
        List<Situation> out = new ArrayList<>();

        // Calculates the distance from the user's location to each situation.
        for (Situation s : all) {
            double d = s.distanceKmTo(lat, lon);

            // Only includes situations with a valid distance inside the selected radius.
            if (!Double.isNaN(d) && d <= radiusKm) {
                out.add(s);
                kmOut.put(s.getId(), d);
            }
        }

        // Sorts the results so the closest situations are shown first.
        Collections.sort(out, (a, b) ->
                Double.compare(kmOut.get(a.getId()), kmOut.get(b.getId())));

        return out;
    }

    /**
     * Gets all road names from the incidents and roadworks lists for search suggestions.
     */
    public static List<String> knownRoads(List<Situation> incidents, List<Situation> roadworks) {
        // TreeSet keeps the road names sorted and removes duplicates.
        TreeSet<String> roads = new TreeSet<>();

        if (incidents != null) for (Situation s : incidents) roads.addAll(s.getRoads());
        if (roadworks != null) for (Situation s : roadworks) roads.addAll(s.getRoads());

        return new ArrayList<>(roads);
    }
}