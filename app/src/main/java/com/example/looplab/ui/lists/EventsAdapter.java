package com.example.looplab.ui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {
    public interface OnEventActionListener {
        void onRegister(Models.EventItem item);
        void onAddToCalendar(Models.EventItem item);
    }

    private final List<Models.EventItem> items = new ArrayList<>();
    private final OnEventActionListener listener;

    public EventsAdapter(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.EventItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }
    
    public void updateItem(Models.EventItem updatedItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(updatedItem.id)) {
                items.set(i, updatedItem);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.EventItem e = items.get(position);
        holder.title.setText(e.title);
        holder.meta.setText(DateFormat.getDateTimeInstance().format(e.startTime));
        
        if (e.registered) {
            holder.btnRegister.setText("✓ Registered");
            holder.btnRegister.setEnabled(false);
            // Set text color to white for better contrast on success background
            holder.btnRegister.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            // Add a subtle shadow for better visibility
            holder.btnRegister.setElevation(2f);
            // Add a subtle border to make it stand out
            holder.btnRegister.setStrokeWidth(2);
            holder.btnRegister.setStrokeColor(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.success)));
        } else {
            holder.btnRegister.setText("Register");
            holder.btnRegister.setEnabled(true);
            // Reset to default text color
            holder.btnRegister.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            // Reset elevation
            holder.btnRegister.setElevation(0f);
            // Reset stroke
            holder.btnRegister.setStrokeWidth(0);
        }
        
        holder.btnRegister.setOnClickListener(v -> listener.onRegister(e));
        holder.btnCalendar.setOnClickListener(v -> listener.onAddToCalendar(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, meta;
        MaterialButton btnRegister, btnCalendar;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            meta = itemView.findViewById(R.id.tvMeta);
            btnRegister = itemView.findViewById(R.id.btnRegister);
            btnCalendar = itemView.findViewById(R.id.btnCalendar);
        }
    }
}


