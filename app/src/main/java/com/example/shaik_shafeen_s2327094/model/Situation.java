package com.example.shaik_shafeen_s2327094.model;

import com.example.shaik_shafeen_s2327094.util.Format;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Stores details about a traffic incident or roadworks entry.
public class Situation {
    public enum Kind { INCIDENT, ROADWORKS }

    private static final Pattern ROAD =
            Pattern.compile("\\b(?:[AM]\\d{1,4}(?:\\(M\\))?|B\\d{2,4})\\b");

    private final Kind kind;
    private String id = "";
    private String recordType = "";
    private String subType = "";
    private String comment = "";
    private String probability = "";
    private String compliance = "";
    private String creationTime = "";
    private String versionTime = "";
    private String startTime = "";
    private String endTime = "";
    private String validityStatus = "";
    private String direction = "";
    private Boolean urgent = null;
    private final Impact impact = new Impact();
    private final List<LocationPoint> points = new ArrayList<>();
    private final Set<String> roads = new LinkedHashSet<>();
    private String searchText = "";

    public Situation(Kind kind) { this.kind = kind; }

    // Getter methods to access the situation details.
    public Kind getKind() { return kind; }
    public String getId() { return id; }
    public String getRecordType() { return recordType; }
    public String getSubType() { return subType; }
    public String getComment() { return comment; }
    public String getProbability() { return probability; }
    public String getCompliance() { return compliance; }
    public String getCreationTime() { return creationTime; }
    public String getVersionTime() { return versionTime; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getValidityStatus() { return validityStatus; }
    public String getDirection() { return direction; }
    public Boolean getUrgent() { return urgent; }
    public Impact getImpact() { return impact; }
    public List<LocationPoint> getPoints() { return points; }
    public Set<String> getRoads() { return roads; }

    // Setter methods used to update the situation details.
    public void setId(String v) { id = v; }
    public void setRecordType(String v) { recordType = v; }
    public void setSubType(String v) { if (subType.isEmpty()) subType = v; }
    public void setComment(String v) { comment = v; }
    public void setProbability(String v) { probability = v; }
    public void setCompliance(String v) { compliance = v; }
    public void setCreationTime(String v) { creationTime = v; }
    public void setVersionTime(String v) { versionTime = v; }
    public void setStartTime(String v) { startTime = v; }
    public void setEndTime(String v) { endTime = v; }
    public void setValidityStatus(String v) { validityStatus = v; }
    public void setDirection(String v) { direction = v; }
    public void setUrgent(Boolean v) { urgent = v; }
    public void addPoint(LocationPoint p) { points.add(p); }

    // Extracts road names once all the record information has been read.
    public void finish() {
        StringBuilder text = new StringBuilder(comment);
        for (LocationPoint p : points) {
            for (String n : p.getNames()) {
                text.append(' ').append(n);
                addRoadsFrom(n);
            }
        }
        addRoadsFrom(comment);
        searchText = text.toString().toLowerCase(Locale.UK);
    }

    private void addRoadsFrom(String s) {
        Matcher m = ROAD.matcher(s);
        while (m.find()) roads.add(m.group().toUpperCase(Locale.UK));
    }

    // Returns a readable label for the situation type.
    public String getTypeLabel() {
        String s = !subType.isEmpty() ? subType : recordType;
        String p = Format.pretty(s);
        return p.isEmpty() ? (kind == Kind.INCIDENT ? "Incident" : "Roadworks") : p;
    }

    // Returns the delay level based on the reported traffic delays.
    public int getDelayLevel() {
        String d = impact.getDelaysType();
        if (d.isEmpty()) return 0;
        if (d.equalsIgnoreCase("veryLongDelays")) return 3;
        if (d.equalsIgnoreCase("longDelays")) return 2;
        return 1;
    }

    // Checks if a situation matches the road search.
    public boolean matchesRoad(String query) {
        if (query == null) return false;
        String q = query.trim().toLowerCase(Locale.UK);
        if (q.isEmpty()) return false;
        if (roads.contains(q.toUpperCase(Locale.UK))) return true;
        return searchText.contains(q);
    }

    // Checks whether the situation has location coordinates.
    public boolean hasCoordinates() {
        for (LocationPoint p : points) if (p.hasCoordinates()) return true;
        return false;
    }

    // Calculates the distance to the nearest available location point.
    public double distanceKmTo(double lat, double lon) {
        double best = Double.NaN;
        for (LocationPoint p : points) {
            if (!p.hasCoordinates()) continue;
            double d = haversineKm(lat, lon, p.getLatitude(), p.getLongitude());
            if (Double.isNaN(best) || d < best) best = d;
        }
        return best;
    }

    // Calculates the distance between two sets of coordinates in kilometres.
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}