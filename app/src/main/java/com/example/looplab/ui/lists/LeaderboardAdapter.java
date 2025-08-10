package com.example.looplab.ui.lists;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
    public static class Row { 
        public int rank; 
        public String name; 
        public int points;
        public int coursesCompleted;
        public int eventsAttended;
        public int lecturesWatched;
    }
    
    private final List<Row> items = new ArrayList<>();
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

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
        Context context = holder.itemView.getContext();
        
        // Set ranking with special colors for top 3
        holder.rank.setText("#" + r.rank);
        setRankingBadgeColor(holder.rankingBadge, r.rank, context);
        
        // Set user name
        holder.name.setText(r.name);
        
        // Set formatted points
        holder.points.setText(numberFormat.format(r.points));
        
        // Set achievements text
        if (r.coursesCompleted > 0 || r.eventsAttended > 0 || r.lecturesWatched > 0) {
            holder.achievements.setVisibility(View.VISIBLE);
            StringBuilder achievementsText = new StringBuilder();
            
            if (r.coursesCompleted > 0) {
                achievementsText.append(r.coursesCompleted).append(" course");
                if (r.coursesCompleted > 1) achievementsText.append("s");
            }
            
            if (r.eventsAttended > 0) {
                if (achievementsText.length() > 0) achievementsText.append(" • ");
                achievementsText.append(r.eventsAttended).append(" event");
                if (r.eventsAttended > 1) achievementsText.append("s");
            }
            
            if (r.lecturesWatched > 0) {
                if (achievementsText.length() > 0) achievementsText.append(" • ");
                achievementsText.append(r.lecturesWatched).append(" lecture");
                if (r.lecturesWatched > 1) achievementsText.append("s");
            }
            
            holder.achievements.setText(achievementsText.toString());
        } else {
            holder.achievements.setVisibility(View.GONE);
        }
    }

    private void setRankingBadgeColor(LinearLayout badge, int rank, Context context) {
        int color;
        switch (rank) {
            case 1: // Gold
                color = Color.parseColor("#FFD700");
                break;
            case 2: // Silver
                color = Color.parseColor("#C0C0C0");
                break;
            case 3: // Bronze
                color = Color.parseColor("#CD7F32");
                break;
            default: // Default blue
                color = ContextCompat.getColor(context, R.color.looplab_blue);
                break;
        }
        badge.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout rankingBadge;
        TextView rank, name, points, achievements;
        
        VH(@NonNull View itemView) {
            super(itemView);
            rankingBadge = itemView.findViewById(R.id.rankingBadge);
            rank = itemView.findViewById(R.id.tvRank);
            name = itemView.findViewById(R.id.tvName);
            points = itemView.findViewById(R.id.tvPoints);
            achievements = itemView.findViewById(R.id.tvAchievements);
        }
    }
}


