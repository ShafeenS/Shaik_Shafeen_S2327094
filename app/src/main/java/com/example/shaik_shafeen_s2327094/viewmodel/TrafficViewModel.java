package com.example.shaik_shafeen_s2327094.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shaik_shafeen_s2327094.data.DatexParser;
import com.example.shaik_shafeen_s2327094.data.FeedData;
import com.example.shaik_shafeen_s2327094.data.TrafficRepository;
import com.example.shaik_shafeen_s2327094.model.Place;
import com.example.shaik_shafeen_s2327094.model.Situation;
import com.example.shaik_shafeen_s2327094.util.SituationFilter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The ViewModel for the app. It manages the traffic data and refreshes it
 * in the background. LiveData is used to send updates to the UI.
 * It also keeps the selected situation when the screen rotates.
 */
public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TrafficViewModel";

    // Data is refreshed every hour.
    public static final long REFRESH_MINUTES =1;

    private final TrafficRepository repository;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    // These hold the main traffic information displayed in the app.
    private final MutableLiveData<List<Situation>> incidents =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Situation>> roadworks =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading...");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Situation> selected = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0);

    // ---------- map state ----------

    public static final int MAP_ALL = 0, MAP_SEARCH = 1, MAP_NEARBY = 2, MAP_SELECTED = 3;
    private final MutableLiveData<Integer> mapMode = new MutableLiveData<>(MAP_ALL);
    private final MutableLiveData<Situation> mapFocus = new MutableLiveData<>(null);
    private boolean hasMapViewport = false;
    private double mapLat, mapLon, mapZoom;

    // ---------- road search state ----------

    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> roadFilter = new MutableLiveData<>(SituationFilter.ALL);
    private final MediatorLiveData<List<Situation>> searchResults = new MediatorLiveData<>();
    private final MediatorLiveData<List<String>> knownRoads = new MediatorLiveData<>();

    // ---------- near me state ----------

    // Available distance options for the nearby search.
    private static final double[] RADIUS_OPTIONS_KM = {1, 2, 5, 10, 25, 50};
    private final MediatorLiveData<List<Situation>> nearby = new MediatorLiveData<>();
    private final Map<String, Double> nearbyKm = new HashMap<>();
    private double nearbyLat = Place.PRESETS[0].latitude;
    private double nearbyLon = Place.PRESETS[0].longitude;
    private String nearbyLabel = Place.PRESETS[0].name;
    private double nearbyRadiusKm = RADIUS_OPTIONS_KM[2];
    private int placeIndex = 0;    // Position in the place spinner.
    private int radiusIndex = 2;   // Position in the radius spinner.

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        // Sets up the search and nearby lists so they update when their data changes.
        searchResults.setValue(new ArrayList<>());
        nearby.setValue(new ArrayList<>());
        knownRoads.setValue(new ArrayList<>());

        searchResults.addSource(incidents, l -> recomputeSearch());
        searchResults.addSource(roadworks, l -> recomputeSearch());
        searchResults.addSource(roadQuery, q -> recomputeSearch());
        searchResults.addSource(roadFilter, f -> recomputeSearch());

        nearby.addSource(incidents, l -> recomputeNearby());
        nearby.addSource(roadworks, l -> recomputeNearby());

        knownRoads.addSource(incidents, l -> recomputeRoads());
        knownRoads.addSource(roadworks, l -> recomputeRoads());

        // Loads the data straight away and then repeats the update every hour.
        scheduler.scheduleWithFixedDelay(this::loadData, 0, REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    // ---------- what the UI observes ----------

    // These methods allow the UI to observe changes without directly changing the data.
    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<Integer> getCurrentTab() { return currentTab; }

    // ---------- actions from the UI ----------

    public void select(Situation s) {
        selected.setValue(s);
    }

    // Changes the selected tab and clears the previous selection if needed.
    public void selectTab(int tab) {
        Integer now = currentTab.getValue();
        if (now != null && now == tab) return;
        currentTab.setValue(tab);
        selected.setValue(null);
    }

    // Allows the user to manually refresh the traffic data.
    public void refresh() {
        scheduler.execute(this::loadData);
    }

    // ---------- map ----------

    public LiveData<Integer> getMapMode() { return mapMode; }
    public LiveData<Situation> getMapFocus() { return mapFocus; }
    public boolean hasMapViewport() { return hasMapViewport; }
    public double getMapLat() { return mapLat; }
    public double getMapLon() { return mapLon; }
    public double getMapZoom() { return mapZoom; }

    public void setMapMode(int mode) {
        Integer now = mapMode.getValue();
        if (now == null || now != mode) mapMode.setValue(mode);
    }

    // Saves the map position so it can be restored when the user returns to the map.
    public void saveMapViewport(double lat, double lon, double zoom) {
        hasMapViewport = true;
        mapLat = lat;
        mapLon = lon;
        mapZoom = zoom;
    }

    // Opens the map and focuses on the selected traffic situation.
    public void showOnMap(Situation s) {
        mapFocus.setValue(s);
        mapMode.setValue(MAP_SELECTED);
        selectTab(4);
    }

    // ---------- road search ----------

    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<Integer> getRoadFilter() { return roadFilter; }
    public LiveData<List<Situation>> getSearchResults() { return searchResults; }
    public LiveData<List<String>> getKnownRoads() { return knownRoads; }

    // Updates the search text.
    public void setRoadQuery(String q) {
        String clean = q == null ? "" : q;
        if (!clean.equals(roadQuery.getValue())) roadQuery.setValue(clean);
    }

    // Changes which type of traffic situation is included in the search.
    public void setRoadFilter(int filter) {
        Integer now = roadFilter.getValue();
        if (now == null || now != filter) roadFilter.setValue(filter);
    }

    // Re-runs the road search using the current search text and filter.
    private void recomputeSearch() {
        searchResults.setValue(SituationFilter.byRoad(incidents.getValue(), roadworks.getValue(),
                roadQuery.getValue(), roadFilter.getValue() == null ? 0 : roadFilter.getValue()));
    }

    // Updates the list of road names used for search suggestions.
    private void recomputeRoads() {
        knownRoads.setValue(SituationFilter.knownRoads(incidents.getValue(), roadworks.getValue()));
    }

    // ---------- near me ----------

    public LiveData<List<Situation>> getNearby() { return nearby; }
    public Double getNearbyKm(String situationId) { return nearbyKm.get(situationId); }
    public String getNearbyLabel() { return nearbyLabel; }
    public double getNearbyRadiusKm() { return nearbyRadiusKm; }
    public double[] getRadiusOptionsKm() { return RADIUS_OPTIONS_KM; }
    public int getPlaceIndex() { return placeIndex; }
    public int getRadiusIndex() { return radiusIndex; }
    public void setPlaceIndex(int i) { placeIndex = i; }

    // Sets the centre point used when searching for nearby situations.
    public void setNearbyCentre(double lat, double lon, String label) {
        nearbyLat = lat;
        nearbyLon = lon;
        nearbyLabel = label;
        recomputeNearby();
    }

    // Updates the search radius using the selected spinner option.
    public void setRadiusIndex(int i) {
        radiusIndex = i;
        nearbyRadiusKm = RADIUS_OPTIONS_KM[i];
        recomputeNearby();
    }

    // Combines incidents and roadworks before filtering them by distance.
    private void recomputeNearby() {
        List<Situation> all = new ArrayList<>();
        if (incidents.getValue() != null) all.addAll(incidents.getValue());
        if (roadworks.getValue() != null) all.addAll(roadworks.getValue());

        nearby.setValue(SituationFilter.nearby(all, nearbyLat, nearbyLon, nearbyRadiusKm, nearbyKm));
    }

    // ---------- background work ----------

    private void loadData() {
        // Handles errors so that one failed update does not stop future scheduled updates.
        try {
            loading.postValue(true);

            // Gets the latest incidents and roadworks from the repository.
            FeedData u = repository.fetchUnplanned();
            FeedData r = repository.fetchRoadworks();
            boolean parseProblem = false;

            if (u.xml != null) {
                try {
                    incidents.postValue(DatexParser.parse(u.xml, Situation.Kind.INCIDENT));
                } catch (Exception e) {
                    Log.e(TAG, "Could not parse unplanned events", e);
                    parseProblem = true;
                }
            }

            if (r.xml != null) {
                try {
                    roadworks.postValue(DatexParser.parse(r.xml, Situation.Kind.ROADWORKS));
                } catch (Exception e) {
                    Log.e(TAG, "Could not parse roadworks", e);
                    parseProblem = true;
                }
            }

            // Updates the status message depending on whether the data loaded successfully.
            status.postValue(buildStatus(u, r, parseProblem));

        } catch (Exception e) {
            Log.e(TAG, "Update failed", e);
            status.postValue("Update failed - showing the last data we have");
        } finally {
            loading.postValue(false);
        }
    }

    // Creates a message to show where the traffic data came from and when it was updated.
    private String buildStatus(FeedData u, FeedData r, boolean parseProblem) {
        if (parseProblem) return "Some data could not be read";

        if (u.source == FeedData.Source.NONE && r.source == FeedData.Source.NONE) {
            return "No internet connection and no saved data";
        }

        // Uses the latest update time from the two feeds.
        long t = Math.max(u.timeMillis, r.timeMillis);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(t));

        if (u.source == FeedData.Source.CACHE || r.source == FeedData.Source.CACHE
                || u.source == FeedData.Source.NONE || r.source == FeedData.Source.NONE) {
            return "Offline - showing data saved at " + time;
        }

        if (u.source == FeedData.Source.SAMPLE || r.source == FeedData.Source.SAMPLE) {
            return "Sample data (no API key set) - " + time;
        }

        return "Live data - updated " + time;
    }

    @Override
    protected void onCleared() {
        // Stops the background scheduler when the ViewModel is no longer needed.
        scheduler.shutdownNow();
        super.onCleared();
    }
}