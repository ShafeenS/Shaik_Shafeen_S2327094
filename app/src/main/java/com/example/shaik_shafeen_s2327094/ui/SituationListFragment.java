package com.example.shaik_shafeen_s2327094.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;

/**
 * Displays a scrolling list of incidents or roadworks depending on which tab is selected.
 */
public class SituationListFragment extends Fragment {

    private static final String ARG_TAB = "tab";

    // Creates a new fragment and passes in which tab should be displayed.
    public static SituationListFragment newInstance(int tab) {
        SituationListFragment f = new SituationListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB, tab);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Loads the layout used for the list.
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Gets the shared ViewModel so the fragment can access the traffic data.
        TrafficViewModel vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        TextView empty = view.findViewById(R.id.emptyView);

        // Sets up the RecyclerView and its adapter.
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        SituationAdapter adapter = new SituationAdapter(vm::select);
        recycler.setAdapter(adapter);

        // Chooses which list to observe based on the tab argument.
        int tab = requireArguments().getInt(ARG_TAB, 0);
        (tab == 1 ? vm.getRoadworks() : vm.getIncidents())
                .observe(getViewLifecycleOwner(), list -> {
                    adapter.submit(list);

                    // Shows a message if there are no items to display.
                    empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                });

        // Updates the selected item in the adapter when the selection changes.
        vm.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getId()));
    }
}