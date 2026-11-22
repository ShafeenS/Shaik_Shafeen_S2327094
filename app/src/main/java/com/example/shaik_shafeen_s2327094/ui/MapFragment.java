package com.example.shaik_shafeen_s2327094.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.model.LocationPoint;
import com.example.shaik_shafeen_s2327094.model.Situation;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {
    private TrafficViewModel vm;
    private MapView map;
    private TextView emptyMessage;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean redrawPending = false;
    private final Map<Long, Drawable> iconCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // osmdroid must be configured before the MapView is created
        Context app = requireContext().getApplicationContext();
        IConfigurationProvider cfg = Configuration.getInstance();
        cfg.setUserAgentValue("GCU-MPD-TrafficScotland-Coursework/1.0 (student project, S2327094)");
        cfg.setOsmdroidBasePath(new File(app.getCacheDir(), "osmdroid"));
        cfg.setOsmdroidTileCache(new File(app.getCacheDir(), "osmdroid/tiles"));
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        map = view.findViewById(R.id.map);
        emptyMessage = view.findViewById(R.id.mapEmptyMessage);
        Spinner modeSpinner = view.findViewById(R.id.mapModeSpinner);

        // Sets up the map tiles and the initial position
        map.setTileSource(new XYTileSource("OSMFrance", 0, 19, 256, ".png",
                new String[]{
                        "https://a.tile.openstreetmap.fr/osmfr/",
                        "https://b.tile.openstreetmap.fr/osmfr/",
                        "https://c.tile.openstreetmap.fr/osmfr/"},
                "© OpenStreetMap contributors"));
        map.setMultiTouchControls(true);
        if (vm.hasMapViewport()) {
            map.getController().setZoom(vm.getMapZoom());
            map.getController().setCenter(new GeoPoint(vm.getMapLat(), vm.getMapLon()));
        } else {
            map.getController().setZoom(7.0);                         // all of Scotland
            map.getController().setCenter(new GeoPoint(56.5, -4.2));
        }

        // Allows the user to choose which situations are displayed on the map
        List<String> modes = Arrays.asList("All situations", "Road search results",
                "Nearby results", "Selected situation");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, modes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);
        Integer mode = vm.getMapMode().getValue();
        modeSpinner.setSelection(mode == null ? 0 : mode);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                vm.setMapMode(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Redraw whenever anything that affects the map changes
        vm.getIncidents().observe(getViewLifecycleOwner(), l -> requestRedraw());
        vm.getRoadworks().observe(getViewLifecycleOwner(), l -> requestRedraw());
        vm.getSearchResults().observe(getViewLifecycleOwner(), l -> requestRedraw());
        vm.getNearby().observe(getViewLifecycleOwner(), l -> requestRedraw());
        vm.getMapMode().observe(getViewLifecycleOwner(), m -> requestRedraw());
        vm.getMapFocus().observe(getViewLifecycleOwner(), s -> requestRedraw());
    }

    /** Several LiveData can change at once; draw only once per UI loop. */
    private void requestRedraw() {
        if (redrawPending) return;
        redrawPending = true;
        handler.post(() -> {
            redrawPending = false;
            if (map != null) redraw();
        });
    }

    // Gets the situations to display based on the selected map mode
    private List<Situation> currentItems() {
        Integer m = vm.getMapMode().getValue();
        int mode = m == null ? TrafficViewModel.MAP_ALL : m;
        List<Situation> items = new ArrayList<>();
        switch (mode) {
            case TrafficViewModel.MAP_SEARCH:
                if (vm.getSearchResults().getValue() != null) items.addAll(vm.getSearchResults().getValue());
                break;
            case TrafficViewModel.MAP_NEARBY:
                if (vm.getNearby().getValue() != null) items.addAll(vm.getNearby().getValue());
                break;
            case TrafficViewModel.MAP_SELECTED:
                if (vm.getMapFocus().getValue() != null) items.add(vm.getMapFocus().getValue());
                break;
            default:
                if (vm.getIncidents().getValue() != null) items.addAll(vm.getIncidents().getValue());
                if (vm.getRoadworks().getValue() != null) items.addAll(vm.getRoadworks().getValue());
                break;
        }
        return items;
    }

    // Clears the old map overlays and adds markers or road sections for the current situations
    private void redraw() {
        Integer m = vm.getMapMode().getValue();
        int mode = m == null ? TrafficViewModel.MAP_ALL : m;
        List<Situation> items = currentItems();

        map.getOverlays().clear();
        List<GeoPoint> allPoints = new ArrayList<>();
        int drawn = 0;

        for (Situation s : items) {
            List<GeoPoint> pts = new ArrayList<>();
            for (LocationPoint p : s.getPoints()) {
                if (p.hasCoordinates()) pts.add(new GeoPoint(p.getLatitude(), p.getLongitude()));
            }
            if (pts.isEmpty()) continue;
            drawn++;
            allPoints.addAll(pts);
            int color = UiUtils.stripColor(requireContext(), s);

            if (pts.size() >= 2) {                       // "from" and "to": draw the stretch
                Polyline line = new Polyline();
                line.setPoints(pts);
                line.getOutlinePaint().setColor(color);
                line.getOutlinePaint().setStrokeWidth(9f);
                line.setOnClickListener((poly, mv, pos) -> {
                    vm.select(s);
                    return true;
                });
                map.getOverlays().add(line);
            }

            Marker marker = new Marker(map);
            marker.setPosition(middle(pts));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setIcon(markerIcon(s));
            marker.setTitle(s.getTypeLabel());
            marker.setOnMarkerClickListener((mk, mv) -> {
                vm.select(s);                            // opens the details
                return true;
            });
            map.getOverlays().add(marker);
        }

        // Displays a message if there are no locations to show
        if (drawn == 0) {
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText(mode == TrafficViewModel.MAP_SEARCH
                    ? "Nothing to show. Search for a road in the Search tab first."
                    : mode == TrafficViewModel.MAP_SELECTED
                    ? "Nothing to show. Open a situation and press \"Show on map\"."
                    : "No situations with a location to show.");
        } else {
            emptyMessage.setVisibility(View.GONE);
        }

        if (mode != TrafficViewModel.MAP_ALL && !allPoints.isEmpty()) zoomTo(allPoints);
        map.invalidate();
    }

    // Adjusts the map zoom so the relevant locations are visible
    private void zoomTo(List<GeoPoint> points) {
        BoundingBox box = BoundingBox.fromGeoPoints(points);
        boolean tiny = box.getLatitudeSpan() < 0.002 && box.getLongitudeSpan() < 0.002;
        map.post(() -> {
            if (map == null) return;
            if (tiny) {
                map.getController().setZoom(15.0);
                map.getController().setCenter(box.getCenterWithDateLine());
            } else {
                map.zoomToBoundingBox(box, false, 100);
            }
        });
    }

    // Finds the middle point to place a marker for a situation
    private static GeoPoint middle(List<GeoPoint> pts) {
        double lat = 0, lon = 0;
        for (GeoPoint p : pts) {
            lat += p.getLatitude();
            lon += p.getLongitude();
        }
        return new GeoPoint(lat / pts.size(), lon / pts.size());
    }

    /** A coloured circle with the situation's icon in white. Cached per icon + colour. */
    private Drawable markerIcon(Situation s) {
        int iconRes = UiUtils.iconFor(s);
        int color = UiUtils.stripColor(requireContext(), s);
        long key = ((long) iconRes << 32) | (color & 0xffffffffL);
        Drawable cached = iconCache.get(key);
        if (cached != null) return cached;

        // Draws the coloured marker background and places the situation icon inside it
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (34 * density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2 * density, paint);

        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(Color.WHITE);
            int pad = size / 5;
            icon.setBounds(pad, pad, size - pad, size - pad);
            icon.draw(canvas);
        }
        Drawable result = new BitmapDrawable(getResources(), bmp);
        iconCache.put(key, result);
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        if (map != null) {
            // Saves the current map position so it can be restored when the user returns
            vm.saveMapViewport(map.getMapCenter().getLatitude(),
                    map.getMapCenter().getLongitude(), map.getZoomLevelDouble());
            map.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        redrawPending = false;
        if (map != null) {
            map.onDetach();
            map = null;
        }
        super.onDestroyView();
    }
}