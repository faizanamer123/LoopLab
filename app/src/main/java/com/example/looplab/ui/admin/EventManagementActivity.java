package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.LinearLayout;

import com.example.looplab.R;
import com.example.looplab.data.EventService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class EventManagementActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CircularProgressIndicator progress;
    private EventsAdapter adapter;
    private final EventService eventService = new EventService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_management);

        rv = findViewById(R.id.rvEvents);
        progress = findViewById(R.id.progressIndicator);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.btnAddEvent);

        adapter = new EventsAdapter(new EventsAdapter.Listener() {
            @Override public void onEdit(Models.EventItem e) { editEvent(e); }
            @Override public void onDelete(Models.EventItem e) { deleteEvent(e); }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> showCreateEventDialog());
        loadEvents();
    }

    private void loadEvents() {
        progress.setVisibility(View.VISIBLE);
        FirebaseRefs.events().orderBy("startTime", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    List<Models.EventItem> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Models.EventItem e = d.toObject(Models.EventItem.class);
                        if (e != null) { e.id = d.getId(); list.add(e); }
                    }
                    adapter.submit(list);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showCreateEventDialog() {
        // Simple inline inputs for title and description
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        v.setPadding(pad, pad, pad, pad);
        EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        EditText etDesc = new EditText(this);
        etDesc.setHint("Description");
        v.addView(etTitle);
        v.addView(etDesc);
        if (etTitle != null) etTitle.setHint("Title");
        if (etDesc != null) etDesc.setHint("Description");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Event")
                .setView(v)
                .setPositiveButton("Create", (d, w) -> {
                    Models.EventItem ev = new Models.EventItem();
                    ev.title = etTitle != null ? etTitle.getText().toString().trim() : "";
                    ev.description = etDesc != null ? etDesc.getText().toString().trim() : "";
                    ev.startTime = System.currentTimeMillis() + 3600_000; // +1h
                    ev.endTime = ev.startTime + 3600_000;
                    ev.location = "Online";
                    ev.organizerId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    ev.organizerName = "Admin";
                    eventService.createEvent(ev, new EventService.EventCallback() {
                        @Override public void onSuccess() { Toast.makeText(EventManagementActivity.this, "Event created", Toast.LENGTH_SHORT).show(); loadEvents(); }
                        @Override public void onError(String error) { Toast.makeText(EventManagementActivity.this, error, Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editEvent(Models.EventItem item) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        v.setPadding(pad, pad, pad, pad);
        EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        etTitle.setText(item.title);
        EditText etDesc = new EditText(this);
        etDesc.setHint("Description");
        etDesc.setText(item.description);
        v.addView(etTitle);
        v.addView(etDesc);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Event")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    item.title = etTitle != null ? etTitle.getText().toString().trim() : item.title;
                    item.description = etDesc != null ? etDesc.getText().toString().trim() : item.description;
                    eventService.updateEvent(item, new EventService.EventCallback() {
                        @Override public void onSuccess() { Toast.makeText(EventManagementActivity.this, "Updated", Toast.LENGTH_SHORT).show(); loadEvents(); }
                        @Override public void onError(String error) { Toast.makeText(EventManagementActivity.this, error, Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(Models.EventItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event")
                .setMessage("Delete '" + item.title + "'?")
                .setPositiveButton("Delete", (d, w) -> eventService.deleteEvent(item.id, new EventService.EventCallback() {
                    @Override public void onSuccess() { Toast.makeText(EventManagementActivity.this, "Deleted", Toast.LENGTH_SHORT).show(); loadEvents(); }
                    @Override public void onError(String error) { Toast.makeText(EventManagementActivity.this, error, Toast.LENGTH_SHORT).show(); }
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class EventsAdapter extends RecyclerView.Adapter<EventVH> {
        interface Listener { void onEdit(Models.EventItem e); void onDelete(Models.EventItem e); }
        private final List<Models.EventItem> data = new ArrayList<>();
        private final Listener listener;
        EventsAdapter(Listener l) { this.listener = l; }
        void submit(List<Models.EventItem> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public EventVH onCreateViewHolder(ViewGroup p, int v) { return new EventVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_event, p, false)); }
        @Override public void onBindViewHolder(EventVH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
    }

    static class EventVH extends RecyclerView.ViewHolder {
        private final android.widget.TextView tvTitle;
        private final android.widget.TextView tvMeta;
        private final android.widget.ImageButton btnEdit;
        private final android.widget.ImageButton btnDelete;
        EventVH(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
        void bind(Models.EventItem e, EventsAdapter.Listener l) {
            tvTitle.setText(e.title);
            tvMeta.setText(e.location + " • " + new java.text.SimpleDateFormat("MMM d, HH:mm").format(new java.util.Date(e.startTime)));
            btnEdit.setOnClickListener(v -> l.onEdit(e));
            btnDelete.setOnClickListener(v -> l.onDelete(e));
        }
    }
}


