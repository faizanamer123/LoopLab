package com.example.looplab.ui.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        MaterialButton btnAddEvent = findViewById(R.id.btnAddEvent);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddEvent = findViewById(R.id.fabAddEvent);

        adapter = new EventsAdapter(new EventsAdapter.Listener() {
            @Override public void onEdit(Models.EventItem e) { editEvent(e); }
            @Override public void onDelete(Models.EventItem e) { deleteEvent(e); }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnAddEvent.setOnClickListener(v -> showCreateEventDialog());
        fabAddEvent.setOnClickListener(v -> showCreateEventDialog());
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
        // Create layout with all input fields
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        v.setPadding(pad, pad, pad, pad);
        
        // Title input
        EditText etTitle = new EditText(this);
        etTitle.setHint("Event Title");
        etTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etTitle.setPadding(pad, pad, pad, pad);
        
        // Description input
        EditText etDesc = new EditText(this);
        etDesc.setHint("Event Description");
        etDesc.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etDesc.setPadding(pad, pad, pad, pad);
        
        // Location input
        EditText etLocation = new EditText(this);
        etLocation.setHint("Event Location");
        etLocation.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etLocation.setPadding(pad, pad, pad, pad);
        
        // Date and time selection buttons
        LinearLayout dateTimeLayout = new LinearLayout(this);
        dateTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        dateTimeLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        MaterialButton btnStartDate = new MaterialButton(this);
        btnStartDate.setText("Start Date & Time");
        btnStartDate.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        ));
        btnStartDate.setStrokeColorResource(R.color.looplab_blue);
        btnStartDate.setStrokeWidth(2);
        
        MaterialButton btnEndDate = new MaterialButton(this);
        btnEndDate.setText("End Date & Time");
        btnEndDate.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        ));
        btnEndDate.setStrokeColorResource(R.color.looplab_blue);
        btnEndDate.setStrokeWidth(2);
        
        dateTimeLayout.addView(btnStartDate);
        dateTimeLayout.addView(btnEndDate);
        
        // Add all views to main layout
        v.addView(etTitle);
        v.addView(etDesc);
        v.addView(etLocation);
        v.addView(dateTimeLayout);
        
        // Variables to store selected dates
        long[] selectedStartTime = {System.currentTimeMillis() + 3600_000}; // +1h default
        long[] selectedEndTime = {System.currentTimeMillis() + 7200_000}; // +2h default
        
        // Start date/time picker
        btnStartDate.setOnClickListener(v1 -> {
            showDateTimePicker(selectedStartTime, btnStartDate, "Start Date & Time");
        });
        
        // End date/time picker
        btnEndDate.setOnClickListener(v1 -> {
            showDateTimePicker(selectedEndTime, btnEndDate, "End Date & Time");
        });
        
        // Update button texts with initial values
        updateDateTimeButtonText(btnStartDate, selectedStartTime[0]);
        updateDateTimeButtonText(btnEndDate, selectedEndTime[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Event")
                .setView(v)
                .setPositiveButton("Create", (d, w) -> {
                    Models.EventItem ev = new Models.EventItem();
                    ev.title = etTitle.getText().toString().trim();
                    ev.description = etDesc.getText().toString().trim();
                    ev.location = etLocation.getText().toString().trim();
                    ev.startTime = selectedStartTime[0];
                    ev.endTime = selectedEndTime[0];
                    ev.organizerId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    ev.organizerName = "Admin";
                    
                    if (ev.title.isEmpty()) {
                        Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    eventService.createEvent(ev, new EventService.EventCallback() {
                        @Override public void onSuccess() { 
                            Toast.makeText(EventManagementActivity.this, "Event created", Toast.LENGTH_SHORT).show(); 
                            loadEvents(); 
                        }
                        @Override public void onError(String error) { 
                            Toast.makeText(EventManagementActivity.this, error, Toast.LENGTH_SHORT).show(); 
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDateTimePicker(long[] selectedTime, MaterialButton button, String title) {
        // Get current date and time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedTime[0]);
        
        // Show date picker first
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // After date is selected, show time picker
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view1, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        
                        selectedTime[0] = calendar.getTimeInMillis();
                        updateDateTimeButtonText(button, selectedTime[0]);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                );
                timePickerDialog.show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateTimeButtonText(MaterialButton button, long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(timestamp));
        button.setText(formattedDate);
    }

    private void editEvent(Models.EventItem item) {
        // Create layout with all input fields
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        v.setPadding(pad, pad, pad, pad);
        
        // Title input
        EditText etTitle = new EditText(this);
        etTitle.setHint("Event Title");
        etTitle.setText(item.title);
        etTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etTitle.setPadding(pad, pad, pad, pad);
        
        // Description input
        EditText etDesc = new EditText(this);
        etDesc.setHint("Event Description");
        etDesc.setText(item.description);
        etDesc.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etDesc.setPadding(pad, pad, pad, pad);
        
        // Location input
        EditText etLocation = new EditText(this);
        etLocation.setHint("Event Location");
        etLocation.setText(item.location != null ? item.location : "");
        etLocation.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        etLocation.setPadding(pad, pad, pad, pad);
        
        // Date and time selection buttons
        LinearLayout dateTimeLayout = new LinearLayout(this);
        dateTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        dateTimeLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        MaterialButton btnStartDate = new MaterialButton(this);
        btnStartDate.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        ));
        btnStartDate.setStrokeColorResource(R.color.looplab_blue);
        btnStartDate.setStrokeWidth(2);
        
        MaterialButton btnEndDate = new MaterialButton(this);
        btnEndDate.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        ));
        btnEndDate.setStrokeColorResource(R.color.looplab_blue);
        btnEndDate.setStrokeWidth(2);
        
        dateTimeLayout.addView(btnStartDate);
        dateTimeLayout.addView(btnEndDate);
        
        // Add all views to main layout
        v.addView(etTitle);
        v.addView(etDesc);
        v.addView(etLocation);
        v.addView(dateTimeLayout);
        
        // Variables to store selected dates
        long[] selectedStartTime = {item.startTime};
        long[] selectedEndTime = {item.endTime};
        
        // Start date/time picker
        btnStartDate.setOnClickListener(v1 -> {
            showDateTimePicker(selectedStartTime, btnStartDate, "Start Date & Time");
        });
        
        // End date/time picker
        btnEndDate.setOnClickListener(v1 -> {
            showDateTimePicker(selectedEndTime, btnEndDate, "End Date & Time");
        });
        
        // Update button texts with current values
        updateDateTimeButtonText(btnStartDate, selectedStartTime[0]);
        updateDateTimeButtonText(btnEndDate, selectedEndTime[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Event")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    item.title = etTitle.getText().toString().trim();
                    item.description = etDesc.getText().toString().trim();
                    item.location = etLocation.getText().toString().trim();
                    item.startTime = selectedStartTime[0];
                    item.endTime = selectedEndTime[0];
                    
                    if (item.title.isEmpty()) {
                        Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    eventService.updateEvent(item, new EventService.EventCallback() {
                        @Override public void onSuccess() { 
                            Toast.makeText(EventManagementActivity.this, "Updated", Toast.LENGTH_SHORT).show(); 
                            loadEvents(); 
                        }
                        @Override public void onError(String error) { 
                            Toast.makeText(EventManagementActivity.this, error, Toast.LENGTH_SHORT).show(); 
                        }
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
            if (tvTitle != null) tvTitle.setText(e.title);
            if (tvMeta != null) tvMeta.setText(e.location + " • " + new java.text.SimpleDateFormat("MMM d, HH:mm").format(new java.util.Date(e.startTime)));
            if (btnEdit != null) btnEdit.setOnClickListener(v -> l.onEdit(e));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> l.onDelete(e));
        }
    }
}


