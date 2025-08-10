package com.example.looplab.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
    public static class Row { public int rank; public String name; public int points; }
    private final List<Row> items = new ArrayList<>();

    public void submit(List<Row> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row r = items.get(position);
        holder.rank.setText("#" + r.rank);
        holder.name.setText(r.name);
        holder.points.setText(r.points + " pts");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView rank, name, points;
        VH(@NonNull View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.tvRank);
            name = itemView.findViewById(R.id.tvName);
            points = itemView.findViewById(R.id.tvPoints);
        }
    }
}


