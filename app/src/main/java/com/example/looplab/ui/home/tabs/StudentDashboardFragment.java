package com.example.looplab.ui.home.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.GamificationService;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.Intent;

public class StudentDashboardFragment extends Fragment {
    
    private MaterialToolbar toolbar;
    private TextView tvWelcomeMessage, tvEnrolledCount, tvCompletedCount, tvPoints, tvBadgeCount, tvRecentActivity;
    private MaterialCardView cardEnrolledCourses, cardCompletedCourses, cardPoints, cardBadges;
    private MaterialButton btnBrowseCourses, btnViewEvents, btnLeaderboard, btnChat;
    
    private CourseService courseService;
    private GamificationService gamificationService;
    private String currentUserId;
    private SimpleDateFormat dateFormat;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);
        
        initializeViews(view);
        setupToolbar();
        setupClickListeners();
        initializeServices();
        loadDashboardData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        tvEnrolledCount = view.findViewById(R.id.tvEnrolledCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvPoints = view.findViewById(R.id.tvPoints);
        tvBadgeCount = view.findViewById(R.id.tvBadgeCount);
        tvRecentActivity = view.findViewById(R.id.tvRecentActivity);
        
        cardEnrolledCourses = view.findViewById(R.id.cardEnrolledCourses);
        cardCompletedCourses = view.findViewById(R.id.cardCompletedCourses);
        cardPoints = view.findViewById(R.id.cardPoints);
        cardBadges = view.findViewById(R.id.cardBadges);
        
        btnBrowseCourses = view.findViewById(R.id.btnBrowseCourses);
        btnViewEvents = view.findViewById(R.id.btnViewEvents);
        btnLeaderboard = view.findViewById(R.id.btnLeaderboard);
        btnChat = view.findViewById(R.id.btnChat);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    private void setupToolbar() {
        // Set welcome message with user's name
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            tvWelcomeMessage.setText("Welcome back, " + user.name + "!");
                        }
                    }
                });
    }
    
    private void setupClickListeners() {
        // Card click listeners
        cardEnrolledCourses.setOnClickListener(v -> {
            // Navigate to enrolled courses
            Intent intent = new Intent(getActivity(), com.example.looplab.ui.courses.CourseManagementActivity.class);
            startActivity(intent);
        });
        
        cardCompletedCourses.setOnClickListener(v -> {
            // Navigate to completed courses
            Intent intent = new Intent(getActivity(), com.example.looplab.ui.courses.CourseManagementActivity.class);
            startActivity(intent);
        });
        
        cardPoints.setOnClickListener(v -> {
            if (getActivity() == null) return;
            ((com.example.looplab.ui.home.HomeActivity) getActivity()).openLeaderboard();
        });
        
        cardBadges.setOnClickListener(v -> {
            if (getActivity() == null) return;
            ((com.example.looplab.ui.home.HomeActivity) getActivity()).openLeaderboard();
        });
        
        // Button click listeners
        btnBrowseCourses.setOnClickListener(v -> {
            // Navigate to courses fragment
            if (getActivity() != null) {
                // Navigate to courses tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(1); // Courses tab
            }
        });
        
        btnViewEvents.setOnClickListener(v -> {
            // Navigate to events fragment
            if (getActivity() != null) {
                // Navigate to events tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(2); // Events tab
            }
        });
        
        btnLeaderboard.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).openLeaderboard();
            }
        });
        
        btnChat.setOnClickListener(v -> {
            // Navigate to chat fragment
            if (getActivity() != null) {
                // Navigate to chat tab in HomeActivity
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(3); // Chat tab
            }
        });
    }
    
    private void initializeServices() {
        courseService = new CourseService();
        gamificationService = new GamificationService();
    }
    
    private void loadDashboardData() {
        loadEnrolledCourses();
        loadCompletedCourses();
        loadPoints();
        loadBadges();
        loadRecentActivity();
    }
    
    private void loadEnrolledCourses() {
        courseService.getUserEnrollments(currentUserId, new CourseService.CourseListCallback() {
            @Override
            public void onSuccess(List<Models.Course> courses) {
                tvEnrolledCount.setText(String.valueOf(courses.size()));
            }
            
            @Override
            public void onError(String error) {
                tvEnrolledCount.setText("0");
                Toast.makeText(getContext(), "Failed to load enrollments", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadCompletedCourses() {
        FirebaseRefs.enrollments().whereEqualTo("userId", currentUserId)
                .whereEqualTo("progress", 100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completedCount = querySnapshot.size();
                    tvCompletedCount.setText(String.valueOf(completedCount));
                })
                .addOnFailureListener(e -> {
                    tvCompletedCount.setText("0");
                });
    }
    
    private void loadPoints() {
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            tvPoints.setText(String.valueOf(user.points));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvPoints.setText("0");
                });
    }
    
    private void loadBadges() {
        gamificationService.getUserBadges(currentUserId, new GamificationService.BadgeCallback() {
            @Override
            public void onSuccess(List<Models.Badge> badges) {
                tvBadgeCount.setText(String.valueOf(badges.size()));
            }
            
            @Override
            public void onError(String error) {
                tvBadgeCount.setText("0");
            }
        });
    }
    
    private void loadRecentActivity() {
        // Load recent progress updates
        FirebaseRefs.progress().whereEqualTo("userId", currentUserId)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Models.Progress latest = null;
                    for (com.google.firebase.firestore.DocumentSnapshot d : querySnapshot.getDocuments()) {
                        Models.Progress p = d.toObject(Models.Progress.class);
                        if (p != null && (latest == null || p.lastWatched > latest.lastWatched)) {
                            latest = p;
                        }
                    }
                    if (latest != null) {
                        String activityText = String.format("Last watched: %s (%s)",
                                latest.lectureId,
                                dateFormat.format(new Date(latest.lastWatched)));
                        tvRecentActivity.setText(activityText);
                    } else {
                        tvRecentActivity.setText("No recent activity");
                    }
                })
                .addOnFailureListener(e -> tvRecentActivity.setText("No recent activity"));
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to the fragment
        loadDashboardData();
    }
}


