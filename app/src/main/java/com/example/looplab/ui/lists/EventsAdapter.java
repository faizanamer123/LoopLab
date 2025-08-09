package com.example.looplab.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;

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
        holder.btnRegister.setText(e.registered ? "Registered" : "Register");
        holder.btnRegister.setEnabled(!e.registered);
        holder.btnRegister.setOnClickListener(v -> listener.onRegister(e));
        holder.btnCalendar.setOnClickListener(v -> listener.onAddToCalendar(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, meta;
        Button btnRegister, btnCalendar;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            meta = itemView.findViewById(R.id.tvMeta);
            btnRegister = itemView.findViewById(R.id.btnRegister);
            btnCalendar = itemView.findViewById(R.id.btnCalendar);
        }
    }
}


