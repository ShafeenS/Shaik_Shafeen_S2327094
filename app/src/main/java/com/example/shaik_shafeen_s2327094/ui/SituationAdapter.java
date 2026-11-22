package com.example.shaik_shafeen_s2327094.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shaik_shafeen_s2327094.R;
import com.example.shaik_shafeen_s2327094.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Shows situations as a list of selectable rows: icon, type and public comment. */
public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.Holder> {

    public interface Listener { void onSituationClicked(Situation s); }

    private final List<Situation> items = new ArrayList<>();
    private final Listener listener;
    private String selectedId = null;
    private Function<Situation, Double> distanceLookup = null;   // optional: km from "you"

    public SituationAdapter(Listener listener) {
        this.listener = listener;
    }

    // Replaces the current list with the latest situations
    public void submit(List<Situation> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    // Sets the function used to get the distance for each situation
    public void setDistanceLookup(Function<Situation, Double> lookup) {
        distanceLookup = lookup;
    }

    // Updates which situation is currently selected
    public void setSelectedId(String id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the layout for each row in the list
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Situation s = items.get(position);

        // Displays the situation details and its matching icon
        h.title.setText(s.getTypeLabel());
        h.comment.setText(s.getComment());
        h.icon.setImageResource(UiUtils.iconFor(s));
        h.strip.setBackgroundColor(UiUtils.stripColor(h.itemView.getContext(), s));

        // Only shows the distance if one has been provided
        Double km = distanceLookup == null ? null : distanceLookup.apply(s);
        if (km == null) {
            h.distance.setVisibility(View.GONE);
        } else {
            h.distance.setVisibility(View.VISIBLE);
            h.distance.setText(String.format(java.util.Locale.UK, "%.1f km away", km));
        }

        h.itemView.setActivated(s.getId().equals(selectedId));
        h.itemView.setOnClickListener(v -> listener.onSituationClicked(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Holds references to the views in each row so they can be reused
    static class Holder extends RecyclerView.ViewHolder {
        final View strip;
        final ImageView icon;
        final TextView title;
        final TextView comment;
        final TextView distance;

        Holder(@NonNull View v) {
            super(v);
            strip = v.findViewById(R.id.strip);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            comment = v.findViewById(R.id.comment);
            distance = v.findViewById(R.id.distance);
        }
    }
}