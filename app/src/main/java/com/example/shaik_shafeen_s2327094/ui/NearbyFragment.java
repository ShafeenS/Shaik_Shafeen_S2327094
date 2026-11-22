package com.example.shaik_shafeen_s2327094.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.model.Place;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Situations within a chosen distance of a place. The place is either one of the
 * fixed places, or the phone's coarse location.
 */
public class NearbyFragment extends Fragment {
    private TrafficViewModel vm;
    private Spinner placeSpinner;

    // Requests location permission before trying to get the phone's location
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) useMyLocation();
                else fallBackToFixedPlace("Location permission denied - using a fixed place");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        placeSpinner = view.findViewById(R.id.placeSpinner);
        Spinner radiusSpinner = view.findViewById(R.id.radiusSpinner);
        TextView summary = view.findViewById(R.id.nearbySummary);
        RecyclerView recycler = view.findViewById(R.id.recycler);

        // Sets up the list of nearby situations
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        SituationAdapter adapter = new SituationAdapter(vm::select);
        adapter.setDistanceLookup(s -> vm.getNearbyKm(s.getId()));
        recycler.setAdapter(adapter);

        // Place choices: the fixed places, then "my location"
        List<String> places = new ArrayList<>();
        for (Place p : Place.PRESETS) places.add(p.name);
        places.add("My current location");
        placeSpinner.setAdapter(spinnerAdapter(places));
        placeSpinner.setSelection(vm.getPlaceIndex());

        // Distance choices
        List<String> radii = new ArrayList<>();
        for (double km : vm.getRadiusOptionsKm()) radii.add((int) km + " km");
        radiusSpinner.setAdapter(spinnerAdapter(radii));
        radiusSpinner.setSelection(vm.getRadiusIndex());

        // Updates the centre point when the user chooses a different place
        placeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                vm.setPlaceIndex(pos);
                if (pos < Place.PRESETS.length) {
                    Place p = Place.PRESETS[pos];
                    vm.setNearbyCentre(p.latitude, p.longitude, p.name);
                } else {
                    useMyLocation();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Updates the search radius when a different distance is selected
        radiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                vm.setRadiusIndex(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Refreshes the list and summary when nearby results change
        vm.getNearby().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            int n = list == null ? 0 : list.size();
            summary.setText(String.format(Locale.UK, "%d situation%s within %d km of %s",
                    n, n == 1 ? "" : "s", (int) vm.getNearbyRadiusKm(), vm.getNearbyLabel()));
        });
        vm.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getId()));
    }

    // Creates the adapter used to display the spinner options
    private ArrayAdapter<String> spinnerAdapter(List<String> items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    /** Uses the last known coarse location of the phone. */
    private void useMyLocation() {
        // Checks whether location permission has already been granted
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }

        // Finds the most recently recorded location from the available providers
        LocationManager lm = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String provider : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException ignored) {
                // this provider needs fine location; skip it
            }
        }

        if (best == null) {
            fallBackToFixedPlace("No location available - using a fixed place");
            return;
        }
        vm.setNearbyCentre(best.getLatitude(), best.getLongitude(), "your location");
    }

    // Uses the first preset place if location access is denied or unavailable
    private void fallBackToFixedPlace(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        placeSpinner.setSelection(0);   // triggers the listener, which sets the fixed place
    }
}