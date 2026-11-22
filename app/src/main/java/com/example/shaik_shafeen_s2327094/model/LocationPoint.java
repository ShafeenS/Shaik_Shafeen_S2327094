package com.example.shaik_shafeen_s2327094.model;

import java.util.ArrayList;
import java.util.List;

// Stores the location details for a situation.
public class LocationPoint {
    private final String role;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private final List<String> names = new ArrayList<>();

    public LocationPoint(String role) { this.role = role; }

    // Getter methods to access the location details.
    public String getRole() { return role; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<String> getNames() { return names; }

    // Updates the coordinates.
    public void setLatitude(double v) { latitude = v; }
    public void setLongitude(double v) { longitude = v; }

    // Adds a location name if it is not already in the list.
    public void addName(String n) {
        if (n != null && !n.isEmpty() && !names.contains(n)) names.add(n);
    }

    // Checks if both coordinates are available.
    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }
}