package com.example.shaik_shafeen_s2327094.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.util.SituationFilter;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;

import java.util.ArrayList;

/** Search incidents and roadworks by road name (e.g. M8, A77) with live results. */
public class SearchFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TrafficViewModel vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        AutoCompleteTextView input = view.findViewById(R.id.roadInput);
        RadioGroup filter = view.findViewById(R.id.filterGroup);
        TextView count = view.findViewById(R.id.resultCount);
        RecyclerView recycler = view.findViewById(R.id.recycler);

        // Sets up the list to display the search results
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        SituationAdapter adapter = new SituationAdapter(vm::select);
        recycler.setAdapter(adapter);

        // Road name suggestions taken from the data
        ArrayAdapter<String> suggestions = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        input.setAdapter(suggestions);
        input.setThreshold(1);

        // Restores the previous search and filter selection
        String q = vm.getRoadQuery().getValue();
        input.setText(q == null ? "" : q, false);
        Integer f = vm.getRoadFilter().getValue();
        filter.check(f == null || f == SituationFilter.ALL ? R.id.filterAll
                : f == SituationFilter.INCIDENTS ? R.id.filterIncidents : R.id.filterRoadworks);

        // Updates the search query whenever the user types
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { vm.setRoadQuery(s.toString()); }
        });

        // Updates the filter depending on which radio button is selected
        filter.setOnCheckedChangeListener((group, id) -> vm.setRoadFilter(
                id == R.id.filterIncidents ? SituationFilter.INCIDENTS
                        : id == R.id.filterRoadworks ? SituationFilter.ROADWORKS
                        : SituationFilter.ALL));

        // Updates the autocomplete suggestions when known roads are loaded
        vm.getKnownRoads().observe(getViewLifecycleOwner(), roads -> {
            suggestions.clear();
            if (roads != null) suggestions.addAll(roads);
            suggestions.notifyDataSetChanged();
        });

        // Displays the matching results and updates the result count message
        vm.getSearchResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            String query = vm.getRoadQuery().getValue();
            if (query == null || query.trim().isEmpty()) {
                count.setText("Type a road name, for example M8 or A77.");
            } else if (list == null || list.isEmpty()) {
                count.setText("No incidents or roadworks found for \"" + query.trim() + "\".");
            } else {
                count.setText(list.size() + " result" + (list.size() == 1 ? "" : "s")
                        + " for \"" + query.trim() + "\"");
            }
        });

        // Highlights the currently selected situation in the list
        vm.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getId()));
    }
}