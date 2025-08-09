package com.example.looplab.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.VH> {
    private final List<Models.Announcement> items = new ArrayList<>();

    public void submit(List<Models.Announcement> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Announcement a = items.get(position);
        holder.title.setText(a.title);
        holder.body.setText(a.body);
        holder.date.setText(DateFormat.getDateTimeInstance().format(a.createdAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, body, date;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            body = itemView.findViewById(R.id.tvBody);
            date = itemView.findViewById(R.id.tvDate);
        }
    }
}


