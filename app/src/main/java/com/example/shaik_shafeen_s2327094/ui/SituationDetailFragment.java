package com.example.shaik_shafeen_s2327094.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.model.Impact;
import com.example.shaik_shafeen_s2327094.model.LocationPoint;
import com.example.shaik_shafeen_s2327094.model.Situation;
import com.example.shaik_shafeen_s2327094.util.Format;
import com.example.shaik_shafeen_s2327094.viewmodel.TrafficViewModel;

import java.util.Locale;

/** Shows everything we know about the selected situation. */
public class SituationDetailFragment extends Fragment {
    private TrafficViewModel vm;
    private View content;
    private TextView emptyHint, typeLabel, kindLabel, comment, delayChip;
    private ImageView icon;
    private LinearLayout sections;
    private Button showOnMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        // Gets references to the views used to display the situation details
        content = view.findViewById(R.id.contentGroup);
        emptyHint = view.findViewById(R.id.emptyHint);
        typeLabel = view.findViewById(R.id.typeLabel);
        kindLabel = view.findViewById(R.id.kindLabel);
        comment = view.findViewById(R.id.detailComment);
        delayChip = view.findViewById(R.id.delayChip);
        icon = view.findViewById(R.id.detailIcon);
        sections = view.findViewById(R.id.sections);
        showOnMap = view.findViewById(R.id.showOnMapButton);

        // The back button is only needed when the detail fills the whole screen (portrait)
        Button back = view.findViewById(R.id.backButton);
        boolean twoPane = requireActivity().findViewById(R.id.detail_container) != null;
        back.setVisibility(twoPane ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> vm.select(null));

        vm.getSelected().observe(getViewLifecycleOwner(), this::bind);
    }

    // Updates the screen whenever a different situation is selected
    private void bind(Situation s) {
        if (s == null) {
            content.setVisibility(View.GONE);
            emptyHint.setVisibility(View.VISIBLE);
            return;
        }
        emptyHint.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        typeLabel.setText(s.getTypeLabel());
        kindLabel.setText(s.getKind() == Situation.Kind.INCIDENT ? "Unplanned incident" : "Roadworks");
        comment.setText(s.getComment());
        icon.setImageResource(UiUtils.iconFor(s));

        // Shows the delay information for roadworks only
        if (s.getKind() == Situation.Kind.ROADWORKS) {
            delayChip.setVisibility(View.VISIBLE);
            delayChip.setText(Format.delayLabel(s.getDelayLevel()));
            delayChip.getBackground().mutate().setTint(
                    UiUtils.delayColor(requireContext(), s.getDelayLevel()));
        } else {
            delayChip.setVisibility(View.GONE);
        }

        showOnMap.setVisibility(s.hasCoordinates() ? View.VISIBLE : View.GONE);
        showOnMap.setOnClickListener(v -> vm.showOnMap(s));

        // Adds the different information sections to the detail screen
        sections.removeAllViews();
        addSection("Impact", impactText(s));
        addSection("When", validityText(s));
        addSection("Where", locationText(s));
        if (!s.getRoads().isEmpty()) addSection("Roads", String.join(", ", s.getRoads()));
        addSection("Reference", referenceText(s));
    }

    // Builds a summary of the delays and lane restrictions
    private String impactText(Situation s) {
        Impact i = s.getImpact();
        StringBuilder sb = new StringBuilder();
        if (!i.getDelaysType().isEmpty()) {
            sb.append("Delays: ").append(Format.pretty(i.getDelaysType())).append('\n');
        } else if (s.getKind() == Situation.Kind.ROADWORKS) {
            sb.append("No delays reported\n");
        }
        if (i.getDelaySeconds() >= 0) {
            sb.append("Expected delay: about ").append(i.getDelaySeconds() / 60).append(" min\n");
        }
        if (i.getLanesRestricted() >= 0) {
            sb.append("Lanes restricted: ").append(i.getLanesRestricted()).append('\n');
        }
        if (i.getOperationalLanes() >= 0) {
            sb.append("Lanes still open: ").append(i.getOperationalLanes()).append('\n');
        }
        String t = sb.toString().trim();
        return t.isEmpty() ? "No impact details provided" : t;
    }

    // Formats the start and end times of the situation
    private String validityText(Situation s) {
        StringBuilder sb = new StringBuilder();
        if (!s.getStartTime().isEmpty()) {
            sb.append("Starts: ").append(Format.time(s.getStartTime())).append('\n');
        }
        if (!s.getEndTime().isEmpty()) {
            sb.append("Ends: ").append(Format.time(s.getEndTime())).append('\n');
        } else {
            sb.append("Ends: not specified\n");
        }
        return sb.toString().trim();
    }

    // Displays the direction and location points for the situation
    private String locationText(Situation s) {
        StringBuilder sb = new StringBuilder();
        if (!s.getDirection().isEmpty()) {
            sb.append("Direction: ").append(Format.direction(s.getDirection())).append('\n');
        }
        for (LocationPoint p : s.getPoints()) {
            String role = p.getRole().equals("from") ? "From"
                    : p.getRole().equals("to") ? "To" : "Point";
            sb.append(role).append(": ");
            if (!p.getNames().isEmpty()) sb.append(String.join(" / ", p.getNames()));
            if (p.hasCoordinates()) {
                sb.append(String.format(Locale.UK, "\n   (%.5f, %.5f)",
                        p.getLatitude(), p.getLongitude()));
            }
            sb.append('\n');
        }
        String t = sb.toString().trim();
        return t.isEmpty() ? "No location details provided" : t;
    }

    // Displays reference information such as the ID and last updated time
    private String referenceText(Situation s) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(s.getId()).append('\n');
        if (!s.getVersionTime().isEmpty()) {
            sb.append("Last updated: ").append(Format.time(s.getVersionTime())).append('\n');
        }
        if (!s.getProbability().isEmpty()) {
            sb.append("Probability: ").append(Format.pretty(s.getProbability())).append('\n');
        }
        if (s.getUrgent() != null) {
            sb.append("Urgent roadworks: ").append(s.getUrgent() ? "yes" : "no").append('\n');
        }
        return sb.toString().trim();
    }

    // Creates a heading and text view for each section
    private void addSection(String title, String body) {
        float d = getResources().getDisplayMetrics().density;
        TextView h = new TextView(requireContext());
        h.setText(title);
        h.setTextSize(16);
        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, (int) (16 * d), 0, (int) (4 * d));
        TextView b = new TextView(requireContext());
        b.setText(body);
        b.setTextSize(15);
        sections.addView(h);
        sections.addView(b);
    }
}