package com.example.looplab.ui.home.tabs;

import android.content.Intent;
import android.provider.CalendarContract;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.EventsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsFragment extends Fragment implements EventsAdapter.OnEventActionListener {

    private EventsAdapter adapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;
    private MaterialButton btnCalendarView, btnMyEvents, btnSort;
    private TextView tvEventsCount;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddEvent;
    private Toolbar toolbar;
    
    private String currentUserId;
    private String currentUserRole;
    private String currentFilter = "all";
    private String searchQuery = "";
    private List<Models.EventItem> allEvents = new ArrayList<>();
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_events, container, false);
        
        initializeViews(root);
        setupToolbar();
        setupClickListeners();
        setupSearchListener();
        setupFilterListeners();
        initializeServices();
        
        return root;
    }

    private void initializeViews(View root) {
        etSearch = root.findViewById(R.id.etSearch);
        chipGroupFilters = root.findViewById(R.id.chipGroupFilters);
        btnCalendarView = root.findViewById(R.id.btnCalendarView);
        btnMyEvents = root.findViewById(R.id.btnMyEvents);
        btnSort = root.findViewById(R.id.btnSort);
        tvEventsCount = root.findViewById(R.id.tvEventsCount);
        progressIndicator = root.findViewById(R.id.progressIndicator);
        emptyState = root.findViewById(R.id.emptyState);
        fabAddEvent = root.findViewById(R.id.fabAddEvent);
        toolbar = root.findViewById(R.id.toolbar);
        
        RecyclerView rv = root.findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventsAdapter(this);
        rv.setAdapter(adapter);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("Events");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupClickListeners() {
        btnCalendarView.setOnClickListener(v -> {
            if (getActivity() == null) return;
            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .setData(android.provider.CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
                        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.getTimeInMillis());
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No calendar app found", Toast.LENGTH_SHORT).show();
            }
        });

        btnMyEvents.setOnClickListener(v -> {
            // Show only user's registered events
            currentFilter = "my";
            updateEventsDisplay();
        });

        btnSort.setOnClickListener(v -> {
            // Show sort options dialog
            showSortDialog();
        });

        fabAddEvent.setOnClickListener(v -> {
            if (getActivity() == null) return;
            if ("admin".equals(currentUserRole) || "teacher".equals(currentUserRole)) {
                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.admin.EventManagementActivity.class));
            } else {
                Toast.makeText(getContext(), "Only admins and teachers can add events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterEvents();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterListeners() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) {
                String chipText = chip.getText().toString().toLowerCase();
                if (chipText.contains("all")) currentFilter = "all";
                else if (chipText.contains("upcoming")) currentFilter = "upcoming";
                else if (chipText.contains("today")) currentFilter = "today";
                else if (chipText.contains("week")) currentFilter = "week";
                
                filterEvents();
            }
        });
    }

    private void initializeServices() {
        loadUserRole();
        loadEvents();
    }

    private void loadUserRole() {
        if (currentUserId == null) return;
        
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            currentUserRole = user.role;
                            updateUIForRole();
                        }
                    }
                });
    }

    private void updateUIForRole() {
        if ("admin".equals(currentUserRole) || "teacher".equals(currentUserRole)) {
            fabAddEvent.setVisibility(View.VISIBLE);
        } else {
            fabAddEvent.setVisibility(View.GONE);
        }
    }

    private void loadEvents() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        FirebaseRefs.events()
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(50)
                .addSnapshotListener((snap, e) -> {
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    
                    if (snap == null || e != null) {
                        showEmptyState();
                        return;
                    }
                    
                    allEvents.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Models.EventItem it = d.toObject(Models.EventItem.class);
                        if (it != null) {
                            it.id = d.getId();
                            // mark registered by attendee list if available
                            if (it.attendees != null && currentUserId != null) {
                                it.registered = it.attendees.contains(currentUserId);
                            }
                            allEvents.add(it);
                        }
                    }
                    
                    filterEvents();
                });
    }

    private void filterEvents() {
        List<Models.EventItem> filteredEvents = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        
        for (Models.EventItem event : allEvents) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                event.title.toLowerCase().contains(searchQuery) ||
                event.description.toLowerCase().contains(searchQuery);
            
            if (!matchesSearch) continue;
            
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "upcoming":
                    matchesFilter = event.startTime > currentTime;
                    break;
                case "today":
                    Calendar eventCal = Calendar.getInstance();
                    eventCal.setTimeInMillis(event.startTime);
                    matchesFilter = isSameDay(calendar, eventCal);
                    break;
                case "week":
                    Calendar weekEnd = Calendar.getInstance();
                    weekEnd.add(Calendar.DAY_OF_YEAR, 7);
                    matchesFilter = event.startTime >= currentTime && event.startTime <= weekEnd.getTimeInMillis();
                    break;
                case "my":
                    matchesFilter = event.registered;
                    break;
            }
            
            if (matchesFilter) {
                filteredEvents.add(event);
            }
        }
        
        updateEventsDisplay(filteredEvents);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void updateEventsDisplay() {
        updateEventsDisplay(null);
    }

    private void updateEventsDisplay(List<Models.EventItem> events) {
        List<Models.EventItem> displayEvents = events != null ? events : allEvents;
        
        if (displayEvents.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.submit(displayEvents);
            updateEventsCount(displayEvents.size());
        }
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
        updateEventsCount(0);
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEventsCount(int count) {
        if (tvEventsCount != null) {
            String countText = count + " Event" + (count != 1 ? "s" : "");
            tvEventsCount.setText(countText);
        }
    }

    private void showSortDialog() {
        // Implement sort dialog
        // For now, just sort by date
        // This would typically show a bottom sheet with sort options
    }

    @Override
    public void onRegister(Models.EventItem item) {
        if (currentUserId == null) return;
        new com.example.looplab.data.EventService().registerForEvent(currentUserId, item.id, new com.example.looplab.data.EventService.RegistrationCallback() {
            @Override
            public void onSuccess(boolean registered) {
                for (Models.EventItem event : allEvents) {
                    if (event.id.equals(item.id)) {
                        event.registered = true;
                        if (event.attendees == null) event.attendees = new java.util.ArrayList<>();
                        if (!event.attendees.contains(currentUserId)) event.attendees.add(currentUserId);
                        break;
                    }
                }
                filterEvents();
            }

            @Override
            public void onError(String error) {
                android.widget.Toast.makeText(getContext(), "Failed to register: " + error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAddToCalendar(Models.EventItem item) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, item.title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, item.description)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.startTime);
            
            if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No calendar app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening calendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserRole();
        loadEvents();
    }
}


