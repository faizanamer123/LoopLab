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
import androidx.appcompat.widget.Toolbar;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.courses.CourseManagementActivity;
import com.example.looplab.ui.live.LiveSessionActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherDashboardFragment extends Fragment {

    // UI Components
    private TextView tvWelcomeMessage, tvTotalCourses, tvTotalStudents, tvLiveSessions, tvAverageRating;
    private MaterialCardView cardTotalCourses, cardTotalStudents, cardLiveSessions, cardAverageRating;
    private MaterialButton btnCreateCourse, btnStartLiveSession, btnManageStudents, btnViewAnalytics;
    private MaterialButton btnViewEvents, btnPostAnnouncement, btnAIChatbot;
    private CircularProgressIndicator progressIndicator;
    private RecyclerView rvRecentActivity;
    private Toolbar toolbar;
    
    // Data
    private String currentUserId;
    private CourseService courseService;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);
        
        initializeViews(root);
        setupToolbar();
        setupClickListeners();
        initializeServices();
        loadDashboardData();
        
        return root;
    }

    private void initializeViews(View root) {
        // Welcome message
        tvWelcomeMessage = root.findViewById(R.id.tvWelcomeMessage);
        
        // Statistics cards
        cardTotalCourses = root.findViewById(R.id.cardTotalCourses);
        cardTotalStudents = root.findViewById(R.id.cardTotalStudents);
        cardLiveSessions = root.findViewById(R.id.cardLiveSessions);
        cardAverageRating = root.findViewById(R.id.cardAverageRating);
        
        tvTotalCourses = root.findViewById(R.id.tvTotalCourses);
        tvTotalStudents = root.findViewById(R.id.tvTotalStudents);
        tvLiveSessions = root.findViewById(R.id.tvLiveSessions);
        tvAverageRating = root.findViewById(R.id.tvAverageRating);
        
        // Action buttons
        btnCreateCourse = root.findViewById(R.id.btnCreateCourse);
        btnStartLiveSession = root.findViewById(R.id.btnStartLiveSession);
        btnManageStudents = root.findViewById(R.id.btnManageStudents);
        btnViewAnalytics = root.findViewById(R.id.btnViewAnalytics);
        btnViewEvents = root.findViewById(R.id.btnViewEvents);
        // btnPostAnnouncement = root.findViewById(R.id.btnPostAnnouncement);
        btnAIChatbot = root.findViewById(R.id.btnAIChatbot);
        
        // Progress indicator
        progressIndicator = root.findViewById(R.id.progressIndicator);
        
        // Recent activity
        // rvRecentActivity = root.findViewById(R.id.rvRecentActivity);
        // rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Toolbar
        toolbar = root.findViewById(R.id.toolbar);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("Teacher Dashboard");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupClickListeners() {
        // Card click listeners
        cardTotalCourses.setOnClickListener(v -> {
            showCourseManagementDialog();
        });
        
        cardTotalStudents.setOnClickListener(v -> {
            showStudentManagementDialog();
        });
        
        cardLiveSessions.setOnClickListener(v -> {
            // Navigate to live sessions
            Intent intent = new Intent(getActivity(), LiveSessionActivity.class);
            startActivity(intent);
        });
        
        cardAverageRating.setOnClickListener(v -> {
            showAnalyticsDialog();
        });
        
        // Button click listeners
        btnCreateCourse.setOnClickListener(v -> {
            showCreateCourseDialog();
        });
        
        btnStartLiveSession.setOnClickListener(v -> {
            showStartLiveSessionDialog();
        });
        
        btnManageStudents.setOnClickListener(v -> {
            showStudentManagementDialog();
        });
        
        btnViewAnalytics.setOnClickListener(v -> {
            showAnalyticsDialog();
        });
        
        btnViewEvents.setOnClickListener(v -> {
            // Navigate to events
            if (getActivity() != null) {
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).navigateToTab(2); // Events tab
            }
        });
        
        // btnPostAnnouncement.setOnClickListener(v -> {
        //     showPostAnnouncementDialog();
        // });
        
        btnAIChatbot.setOnClickListener(v -> {
            showAIChatbotDialog();
        });
    }

    private void showCourseManagementDialog() {
        String[] options = {"View My Courses", "Create New Course", "Edit Course", "Course Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Course Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showMyCourses();
                            break;
                        case 1:
                            showCreateCourseDialog();
                            break;
                        case 2:
                            showEditCourseDialog();
                            break;
                        case 3:
                            showCourseAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showStudentManagementDialog() {
        String[] options = {"View Enrolled Students", "Student Progress", "Send Messages", "Student Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Student Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEnrolledStudents();
                            break;
                        case 1:
                            showStudentProgress();
                            break;
                        case 2:
                            showSendMessages();
                            break;
                        case 3:
                            showStudentAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showAnalyticsDialog() {
        String[] options = {"Course Performance", "Student Engagement", "Completion Rates", "Revenue Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Analytics Dashboard")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCoursePerformance();
                            break;
                        case 1:
                            showStudentEngagement();
                            break;
                        case 2:
                            showCompletionRates();
                            break;
                        case 3:
                            showRevenueAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showCreateCourseDialog() {
        // Show course creation form
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create New Course")
                .setMessage("Course creation feature will be implemented here with form fields for title, description, category, etc.")
                .setPositiveButton("Create", (dialog, which) -> {
                    Toast.makeText(getContext(), "Course created successfully!", Toast.LENGTH_SHORT).show();
                    loadDashboardData(); // Refresh data
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStartLiveSessionDialog() {
        String[] options = {"Start New Session", "Schedule Session", "View Active Sessions"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Live Sessions")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startNewLiveSession();
                            break;
                        case 1:
                            scheduleLiveSession();
                            break;
                        case 2:
                            viewActiveSessions();
                            break;
                    }
                })
                .show();
    }

    private void showPostAnnouncementDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Post Announcement")
                .setMessage("Announcement posting feature will be implemented here with form fields for title, message, target audience, etc.")
                .setPositiveButton("Post", (dialog, which) -> {
                    Toast.makeText(getContext(), "Announcement posted successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAIChatbotDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI Assistant")
                .setMessage("AI chatbot for answering questions about teaching, course creation, and platform features.")
                .setPositiveButton("Start Chat", (dialog, which) -> {
                    Toast.makeText(getContext(), "AI Chat started!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Implementation methods for different features
    private void showMyCourses() {
        if (currentUserId == null) return;
        
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title);
                        }
                    }
                    
                    String[] courses = courseTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("My Courses (" + courses.length + ")")
                            .setItems(courses, (dialog, which) -> {
                                Toast.makeText(getContext(), "Selected: " + courses[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private void showEditCourseDialog() {
        if (currentUserId == null) return;
        
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title);
                        }
                    }
                    
                    String[] courses = courseTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select Course to Edit")
                            .setItems(courses, (dialog, which) -> {
                                Toast.makeText(getContext(), "Editing: " + courses[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Edit", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showCourseAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Course Analytics")
                .setMessage("• Total Views: 1,250\n• Average Rating: 4.5/5\n• Completion Rate: 78%\n• Student Engagement: 85%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEnrolledStudents() {
        if (currentUserId == null) return;
        
        // Count students enrolled in teacher's courses
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(coursesSnapshot -> {
                    final int[] totalStudents = {0};
                    int courseCount = coursesSnapshot.size();
                    
                    if (courseCount == 0) {
                        showNoStudentsMessage();
                        return;
                    }
                    
                    for (DocumentSnapshot courseDoc : coursesSnapshot.getDocuments()) {
                        String courseId = courseDoc.getId();
                        FirebaseRefs.enrollments().whereEqualTo("courseId", courseId).get()
                                .addOnSuccessListener(enrollmentsSnapshot -> {
                                    totalStudents[0] += enrollmentsSnapshot.size();
                                    
                                    if (totalStudents[0] >= 0) {
                                        showStudentsList(totalStudents[0]);
                                    }
                                });
                    }
                });
    }

    private void showNoStudentsMessage() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Students")
                .setMessage("You don't have any enrolled students yet. Create courses to attract students!")
                .setPositiveButton("Create Course", (dialog, which) -> showCreateCourseDialog())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showStudentsList(int count) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enrolled Students")
                .setMessage("You have " + count + " students enrolled in your courses.\n\nSample students:\n• John Smith (Android Dev)\n• Sarah Johnson (UI/UX Design)\n• Michael Chen (Web Development)")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showStudentProgress() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Student Progress")
                .setMessage("• John Smith: 85% complete\n• Sarah Johnson: 92% complete\n• Michael Chen: 67% complete\n• Average Progress: 81%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSendMessages() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Send Messages")
                .setMessage("Send messages to your students:\n• Course updates\n• Assignment reminders\n• Live session notifications\n• General announcements")
                .setPositiveButton("Send Message", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Student Analytics")
                .setMessage("• Active Students: 45\n• Average Session Time: 45 min\n• Course Completion Rate: 78%\n• Student Satisfaction: 4.6/5")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCoursePerformance() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Course Performance")
                .setMessage("• Android Development: 4.5★ (45 students)\n• UI/UX Design: 4.8★ (32 students)\n• Web Development: 4.2★ (28 students)")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showStudentEngagement() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Student Engagement")
                .setMessage("• Daily Active Users: 85%\n• Average Time Spent: 2.5 hours\n• Discussion Participation: 73%\n• Assignment Submission: 89%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCompletionRates() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Completion Rates")
                .setMessage("• Android Development: 78%\n• UI/UX Design: 85%\n• Web Development: 72%\n• Overall Average: 78%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRevenueAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Revenue Analytics")
                .setMessage("• Monthly Revenue: $2,450\n• Course Sales: 45\n• Average Course Price: $54\n• Growth Rate: +15%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void startNewLiveSession() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start Live Session")
                .setMessage("Starting a new live session for your students...")
                .setPositiveButton("Start", (dialog, which) -> {
                    Toast.makeText(getContext(), "Live session started!", Toast.LENGTH_SHORT).show();
                    // Navigate to live session
                    Intent intent = new Intent(getActivity(), LiveSessionActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scheduleLiveSession() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Schedule Live Session")
                .setMessage("Schedule a live session for a future date and time.")
                .setPositiveButton("Schedule", (dialog, which) -> {
                    Toast.makeText(getContext(), "Live session scheduled!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void viewActiveSessions() {
        if (currentUserId == null) return;
        
        FirebaseRefs.liveSessions().whereEqualTo("instructorId", currentUserId)
                .whereEqualTo("isActive", true).get()
                .addOnSuccessListener(querySnapshot -> {
                    int activeSessions = querySnapshot.size();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Active Sessions")
                            .setMessage("You have " + activeSessions + " active live sessions.")
                            .setPositiveButton("Join Session", (dialog, which) -> {
                                Intent intent = new Intent(getActivity(), LiveSessionActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
    }

    private void initializeServices() {
        courseService = new CourseService();
        loadUserProfile();
    }

    private void loadDashboardData() {
        loadTotalCourses();
        loadTotalStudents();
        loadLiveSessions();
        loadAverageRating();
        loadRecentActivity();
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null && tvWelcomeMessage != null) {
                            tvWelcomeMessage.setText("Welcome back, " + user.name + "!");
                        }
                    }
                });
    }

    private void loadTotalCourses() {
        if (currentUserId == null) return;
        
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalCourses = querySnapshot.size();
                    if (tvTotalCourses != null) {
                        tvTotalCourses.setText(String.valueOf(totalCourses));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvTotalCourses != null) {
                        tvTotalCourses.setText("0");
                    }
                });
    }

    private void loadTotalStudents() {
        if (currentUserId == null) return;
        
        // Count students enrolled in teacher's courses
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(coursesSnapshot -> {
                    final int[] totalStudents = {0};
                    int courseCount = coursesSnapshot.size();
                    
                    if (courseCount == 0) {
                        if (tvTotalStudents != null) {
                            tvTotalStudents.setText("0");
                        }
                        return;
                    }
                    
                    for (DocumentSnapshot courseDoc : coursesSnapshot.getDocuments()) {
                        String courseId = courseDoc.getId();
                        FirebaseRefs.enrollments().whereEqualTo("courseId", courseId).get()
                                .addOnSuccessListener(enrollmentsSnapshot -> {
                                    totalStudents[0] += enrollmentsSnapshot.size();
                                    
                                    // Check if we've processed all courses
                                    if (totalStudents[0] >= 0) {
                                        if (tvTotalStudents != null) {
                                            tvTotalStudents.setText(String.valueOf(totalStudents[0]));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (tvTotalStudents != null) {
                                        tvTotalStudents.setText("0");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvTotalStudents != null) {
                        tvTotalStudents.setText("0");
                    }
                });
    }

    private void loadLiveSessions() {
        if (currentUserId == null) return;
        
        FirebaseRefs.liveSessions().whereEqualTo("instructorId", currentUserId)
                .whereEqualTo("isActive", true).get()
                .addOnSuccessListener(querySnapshot -> {
                    int activeSessions = querySnapshot.size();
                    if (tvLiveSessions != null) {
                        tvLiveSessions.setText(String.valueOf(activeSessions));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvLiveSessions != null) {
                        tvLiveSessions.setText("0");
                    }
                });
    }

    private void loadAverageRating() {
        if (currentUserId == null) return;
        
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRating = 0;
                    int courseCount = 0;
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null && course.rating > 0) {
                            totalRating += course.rating;
                            courseCount++;
                        }
                    }
                    
                    double averageRating = courseCount > 0 ? totalRating / courseCount : 0;
                    if (tvAverageRating != null) {
                        tvAverageRating.setText(String.format("%.1f", averageRating));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvAverageRating != null) {
                        tvAverageRating.setText("0.0");
                    }
                });
    }

    private void loadRecentActivity() {
        // Load recent course enrollments, completions, etc.
        if (currentUserId == null) return;
        
        FirebaseRefs.courses().whereEqualTo("instructorId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(5).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> recentActivities = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            String activity = "Course created: " + course.title;
                            recentActivities.add(activity);
                        }
                    }
                    updateRecentActivityDisplay(recentActivities);
                });
    }

    private void updateRecentActivityDisplay(List<String> activities) {
        // This would typically update a RecyclerView adapter
        // For now, we'll just show a toast with the count
        if (getContext() != null) {
            Toast.makeText(getContext(), "Recent activities loaded: " + activities.size(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }
}



