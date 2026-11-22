/*  Mobile Platform Development - 1st diet 26/27 - Traffic Scotland app
    Name                 Shafeen Shaik
    Student ID           S2327094
    Programme of Study   Software Development
*/
package com.example.shaik_shafeen_s2327094;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.shaik_shafeen_s2327094.ui.MapFragment;
import com.example.shaik_shafeen_s2327094.ui.NearbyFragment;
import com.example.shaik_shafeen_s2327094.ui.SearchFragment;
import com.example.shaik_shafeen_s2327094.ui.SituationDetailFragment;
import com.example.shaik_shafeen_s2327094.ui.SituationListFragment;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;
import com.google.android.material.navigation.NavigationBarView;

import java.util.function.Supplier;

/**
 * The single activity. It only decides which fragments are on screen:
 *  - portrait: one fragment at a time (the list, or the detail of the selected item)
 *  - landscape: list on the left and detail on the right at the same time
 * All data and the current selection live in the TrafficViewModel.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG_DETAIL = "detail";            // portrait
    private static final String TAG_PANE_DETAIL = "pane_detail";  // landscape

    private TrafficViewModel vm;
    private boolean twoPane;
    private NavigationBarView navigation;
    private OnBackPressedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        vm = new ViewModelProvider(this).get(TrafficViewModel.class);
        twoPane = findViewById(R.id.detail_container) != null;

        TextView statusText = findViewById(R.id.statusText);
        ProgressBar progress = findViewById(R.id.progress);
        findViewById(R.id.refreshButton).setOnClickListener(v -> vm.refresh());

        navigation = findViewById(R.id.navigation);
        navigation.setOnItemSelectedListener(item -> {
            vm.selectTab(tabForItem(item.getItemId()));
            return true;
        });

        // In portrait, Back closes the detail and returns to the list
        backCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { vm.select(null); }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        vm.getStatus().observe(this, statusText::setText);
        vm.getLoading().observe(this,
                busy -> progress.setVisibility(Boolean.TRUE.equals(busy) ? View.VISIBLE : View.GONE));
        vm.getCurrentTab().observe(this, tab -> render());
        vm.getSelected().observe(this, selected -> render());
    }

    private void render() {
        Integer t = vm.getCurrentTab().getValue();
        int tab = t == null ? 0 : t;
        boolean hasSelection = vm.getSelected().getValue() != null;
        FragmentManager fm = getSupportFragmentManager();

        int wanted = itemForTab(tab);
        if (navigation.getSelectedItemId() != wanted) navigation.setSelectedItemId(wanted);

        String listTag = "tab_" + tab;
        Supplier<Fragment> listFactory = () -> fragmentForTab(tab);

        if (twoPane) {
            show(R.id.fragment_container, listTag, listFactory);
            show(R.id.detail_container, TAG_PANE_DETAIL, SituationDetailFragment::new);
            backCallback.setEnabled(false);
        } else {
            Fragment stale = fm.findFragmentByTag(TAG_PANE_DETAIL);
            if (stale != null) fm.beginTransaction().remove(stale).commit();
            if (hasSelection) {
                show(R.id.fragment_container, TAG_DETAIL, SituationDetailFragment::new);
            } else {
                show(R.id.fragment_container, listTag, listFactory);
            }
            backCallback.setEnabled(hasSelection);
        }
    }

    // Tabs: 0 incidents, 1 roadworks, 2 road search, 3 nearby, 4 map
    private static int tabForItem(int itemId) {
        if (itemId == R.id.nav_roadworks) return 1;
        if (itemId == R.id.nav_search) return 2;
        if (itemId == R.id.nav_nearby) return 3;
        if (itemId == R.id.nav_map) return 4;
        return 0;
    }

    private static int itemForTab(int tab) {
        switch (tab) {
            case 1:  return R.id.nav_roadworks;
            case 2:  return R.id.nav_search;
            case 3:  return R.id.nav_nearby;
            case 4:  return R.id.nav_map;
            default: return R.id.nav_incidents;
        }
    }

    private static Fragment fragmentForTab(int tab) {
        switch (tab) {
            case 2:  return new SearchFragment();
            case 3:  return new NearbyFragment();
            case 4:  return new MapFragment();
            default: return SituationListFragment.newInstance(tab);
        }
    }

    /** Puts a fragment in a container unless that fragment is already there. */
    private void show(int containerId, String tag, Supplier<Fragment> factory) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentById(containerId);
        if (existing != null && tag.equals(existing.getTag())) return;
        fm.beginTransaction().replace(containerId, factory.get(), tag).commit();
    }
}
