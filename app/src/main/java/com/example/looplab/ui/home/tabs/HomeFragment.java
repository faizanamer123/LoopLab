package com.example.looplab.ui.home.tabs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.looplab.ui.lists.AnnouncementsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private AnnouncementsAdapter adapter;
    private TextView tvWelcomeMessage;
    private MaterialButton btnBrowseCourses, btnViewEvents, btnJoinChat, btnLeaderboard, btnViewAllAnnouncements;
    private CircularProgressIndicator progressIndicator;
    private Toolbar toolbar;
    private String currentUserId;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        
        initializeViews(root);
        setupToolbar();
        setupClickListeners();
        initializeServices();
        
        return root;
    }

    private void initializeViews(View root) {
        tvWelcomeMessage = root.findViewById(R.id.tvWelcomeMessage);
        btnBrowseCourses = root.findViewById(R.id.btnBrowseCourses);
        btnViewEvents = root.findViewById(R.id.btnViewEvents);
        btnJoinChat = root.findViewById(R.id.btnJoinChat);
        btnLeaderboard = root.findViewById(R.id.btnLeaderboard);
        btnViewAllAnnouncements = root.findViewById(R.id.btnViewAllAnnouncements);
        progressIndicator = root.findViewById(R.id.progressIndicator);
        toolbar = root.findViewById(R.id.toolbar);
        
        RecyclerView rv = root.findViewById(R.id.rvAnnouncements);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new AnnouncementsAdapter();
            rv.setAdapter(adapter);
        }
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("LoopLab Home");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupClickListeners() {
        btnBrowseCourses.setOnClickListener(v -> {
            // Navigate to courses section
            if (getActivity() != null) {
                // Navigate to courses tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(1); // Courses tab
            }
        });

        btnViewEvents.setOnClickListener(v -> {
            // Navigate to events section
            if (getActivity() != null) {
                // Navigate to events tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(2); // Events tab
            }
        });

        btnJoinChat.setOnClickListener(v -> {
            // Navigate to chat section
            if (getActivity() != null) {
                // Navigate to chat tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(3); // Chat tab
            }
        });

        btnLeaderboard.setOnClickListener(v -> {
            // Navigate to leaderboard
            if (getActivity() != null) {
                // Navigate to leaderboard tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(4); // Profile tab (leaderboard is in profile)
            }
        });

        btnViewAllAnnouncements.setOnClickListener(v -> {
            if (getActivity() == null) return;
            // For now just navigate to Chat tab as a placeholder for an announcements screen
            ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(0);
        });
    }

    private void initializeServices() {
        loadUserProfile();
        loadAnnouncements();
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null && tvWelcomeMessage != null) {
                            String welcomeText = "Welcome back, " + user.name + "!";
                            tvWelcomeMessage.setText(welcomeText);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error silently
                });
    }

    private void loadAnnouncements() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        FirebaseRefs.announcements()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5) // Show only 5 latest announcements
                .addSnapshotListener((snap, e) -> {
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    
                    if (snap == null || e != null) {
                        // Handle error
                        return;
                    }
                    
                    List<Models.Announcement> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Models.Announcement it = d.toObject(Models.Announcement.class);
                        if (it != null) {
                            it.id = d.getId();
                            list.add(it);
                        }
                    }
                    
                    if (adapter != null) {
                        adapter.submit(list);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadAnnouncements();
    }
}


