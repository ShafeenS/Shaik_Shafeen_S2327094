package com.example.shaik_shafeen_s2327094.model;

// Stores preset locations that users can search around.
public class Place {
    public static final Place[] PRESETS = {
            new Place("Glasgow Caledonian University", 55.8668, -4.2503),
            new Place("Edinburgh city centre", 55.9533, -3.1883),
            new Place("Stirling", 56.1165, -3.9369),
            new Place("Dundee", 56.4620, -2.9707),
            new Place("Aberdeen", 57.1497, -2.0943),
            new Place("Inverness", 57.4778, -4.2247)
    };

    public final String name;
    public final double latitude;
    public final double longitude;

    // Creates a place with its name and coordinates.
    public Place(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}