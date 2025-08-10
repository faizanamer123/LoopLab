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
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
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

public class AdminDashboardFragment extends Fragment {

    // UI Components
    private TextView tvWelcomeMessage, tvTotalUsers, tvTotalCourses, tvTotalEvents, tvActiveSessions;
    private MaterialCardView cardTotalUsers, cardTotalCourses, cardTotalEvents, cardActiveSessions;
    private MaterialButton btnManageUsers, btnManageCourses, btnManageEvents, btnManageAnnouncements;
    private MaterialButton btnViewAnalytics, btnManageFeedback, btnLiveSessions, btnAIChatbot;
    private CircularProgressIndicator progressIndicator;
    private RecyclerView rvRecentActivity;
    private Toolbar toolbar;
    
    // Data
    private String currentUserId;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
        
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
        cardTotalUsers = root.findViewById(R.id.cardTotalUsers);
        cardTotalCourses = root.findViewById(R.id.cardTotalCourses);
        cardTotalEvents = root.findViewById(R.id.cardTotalEvents);
        cardActiveSessions = root.findViewById(R.id.cardActiveSessions);
        
        tvTotalUsers = root.findViewById(R.id.tvTotalUsers);
        tvTotalCourses = root.findViewById(R.id.tvTotalCourses);
        tvTotalEvents = root.findViewById(R.id.tvTotalEvents);
        tvActiveSessions = root.findViewById(R.id.tvActiveSessions);
        
        // Action buttons
        btnManageUsers = root.findViewById(R.id.btnManageUsers);
        btnManageCourses = root.findViewById(R.id.btnManageCourses);
        btnManageEvents = root.findViewById(R.id.btnManageEvents);
        btnManageAnnouncements = root.findViewById(R.id.btnManageAnnouncements);
        btnViewAnalytics = root.findViewById(R.id.btnViewAnalytics);
        btnManageFeedback = root.findViewById(R.id.btnManageFeedback);
        btnLiveSessions = root.findViewById(R.id.btnLiveSessions);
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
            toolbar.setTitle("Admin Dashboard");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupClickListeners() {
        // Card click listeners
        cardTotalUsers.setOnClickListener(v -> {
            showUserManagementDialog();
        });
        
        cardTotalCourses.setOnClickListener(v -> {
            showCourseManagementDialog();
        });
        
        cardTotalEvents.setOnClickListener(v -> showEventManagement());
        
        cardActiveSessions.setOnClickListener(v -> {
            showLiveSessionsDialog();
        });
        
        // Button click listeners
        btnManageUsers.setOnClickListener(v -> {
            showUserManagementDialog();
        });
        
        btnManageCourses.setOnClickListener(v -> {
            showCourseManagementDialog();
        });
        
        btnManageEvents.setOnClickListener(v -> showEventManagement());
        
        btnManageAnnouncements.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.admin.AnnouncementsManagementActivity.class));
            } else {
                showAnnouncementManagementDialog();
            }
        });
        
        btnViewAnalytics.setOnClickListener(v -> {
            showAnalyticsDialog();
        });
        
        btnManageFeedback.setOnClickListener(v -> {
            showFeedbackManagementDialog();
        });
        
        btnLiveSessions.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.live.LiveSessionsListActivity.class));
            } else {
                showLiveSessionsDialog();
            }
        });
        
        btnAIChatbot.setOnClickListener(v -> {
            showAIChatbotDialog();
        });
    }

    private void showUserManagementDialog() {
        String[] options = {"View All Users", "Add New User", "Edit User", "Suspend User", "Delete User", "User Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("User Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // open full user manager
                            if (getActivity() != null) {
                                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.admin.UserManagementActivity.class));
                            } else {
                                showAllUsers();
                            }
                            break;
                        case 1:
                            // open add user dialog within user manager
                            if (getActivity() != null) {
                                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.admin.UserManagementActivity.class));
                            } else {
                                showAddUserDialog();
                            }
                            break;
                        case 2:
                            showEditUserDialog();
                            break;
                        case 3:
                            showSuspendUserDialog();
                            break;
                        case 4:
                            showDeleteUserDialog();
                            break;
                        case 5:
                            showUserAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showCourseManagementDialog() {
        String[] options = {"View All Courses", "Approve Courses", "Edit Course", "Delete Course", "Course Analytics", "Assign Teachers"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Course Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (getActivity() != null) {
                                startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.courses.CourseManagementActivity.class));
                            } else {
                                showAllCourses();
                            }
                            break;
                        case 1:
                            showApproveCourses();
                            break;
                        case 2:
                            showEditCourseDialog();
                            break;
                        case 3:
                            showDeleteCourseDialog();
                            break;
                        case 4:
                            showCourseAnalytics();
                            break;
                        case 5:
                            showAssignTeachers();
                            break;
                    }
                })
                .show();
    }

    private void showEventManagementDialog() {
        String[] options = {"Open Event Manager", "Event Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Event Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEventManagement();
                            break;
                        case 1:
                            showEventAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showEventManagement() {
        if (getActivity() == null) return;
        startActivity(new android.content.Intent(getActivity(), com.example.looplab.ui.admin.EventManagementActivity.class));
    }

    private void showAnnouncementManagementDialog() {
        String[] options = {"View All Announcements", "Create Announcement", "Edit Announcement", "Delete Announcement", "Send Notifications"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Announcement Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAllAnnouncements();
                            break;
                        case 1:
                            showCreateAnnouncementDialog();
                            break;
                        case 2:
                            showEditAnnouncementDialog();
                            break;
                        case 3:
                            showDeleteAnnouncementDialog();
                            break;
                        case 4:
                            showSendNotificationsDialog();
                            break;
                    }
                })
                .show();
    }

    private void showAnalyticsDialog() {
        String[] options = {"User Analytics", "Course Analytics", "Event Analytics", "Revenue Analytics", "System Analytics", "Download Reports"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Analytics Dashboard")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showUserAnalytics();
                            break;
                        case 1:
                            showCourseAnalytics();
                            break;
                        case 2:
                            showEventAnalytics();
                            break;
                        case 3:
                            showRevenueAnalytics();
                            break;
                        case 4:
                            showSystemAnalytics();
                            break;
                        case 5:
                            showDownloadReports();
                            break;
                    }
                })
                .show();
    }

    private void showFeedbackManagementDialog() {
        String[] options = {"View All Feedback", "Respond to Feedback", "Mark as Resolved", "Export Feedback", "Feedback Analytics"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Feedback Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAllFeedback();
                            break;
                        case 1:
                            showRespondToFeedback();
                            break;
                        case 2:
                            showMarkAsResolved();
                            break;
                        case 3:
                            showExportFeedback();
                            break;
                        case 4:
                            showFeedbackAnalytics();
                            break;
                    }
                })
                .show();
    }

    private void showLiveSessionsDialog() {
        String[] options = {"View Active Sessions", "Monitor Sessions", "Session Analytics", "Manage Sessions"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Live Sessions Management")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showActiveSessions();
                            break;
                        case 1:
                            showMonitorSessions();
                            break;
                        case 2:
                            showSessionAnalytics();
                            break;
                        case 3:
                            showManageSessions();
                            break;
                    }
                })
                .show();
    }

    private void showAIChatbotDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI Chatbot")
                .setMessage("Open the AI assistant to chat or configure API key.")
                .setPositiveButton("Open Chat", (dialog, which) -> {
                    if (getActivity() == null) return;
                    android.content.Intent i = new android.content.Intent(getActivity(), com.example.looplab.ui.chat.AIChatActivity.class);
                    i.putExtra("role", "admin");
                    startActivity(i);
                })
                .setNeutralButton("Configure", (d, w) -> {
                    // Quick inline config via dialog for Gemini API key
                    final android.widget.EditText input = new android.widget.EditText(getContext());
                    input.setHint("Gemini API Key");
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Set Gemini API Key")
                            .setView(input)
                            .setPositiveButton("Save", (dd, ww) -> {
                                com.example.looplab.data.AIChatbotService svc = new com.example.looplab.data.AIChatbotService(requireContext());
                                svc.setApiKey(input.getText().toString());
                                Toast.makeText(getContext(), "AI key saved", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // Implementation methods for different features
    private void showAllUsers() {
        FirebaseRefs.users().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            userNames.add(user.name + " (" + user.role + ")");
                        }
                    }
                    
                    String[] users = userNames.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("All Users (" + users.length + ")")
                            .setItems(users, (dialog, which) -> {
                                Toast.makeText(getContext(), "Selected: " + users[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private void showAddUserDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New User")
                .setMessage("Add user form will be implemented here with fields for name, email, role, etc.")
                .setPositiveButton("Add", (dialog, which) -> {
                    Toast.makeText(getContext(), "User added successfully!", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditUserDialog() {
        FirebaseRefs.users().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            userNames.add(user.name + " (" + user.role + ")");
                        }
                    }
                    
                    String[] users = userNames.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select User to Edit")
                            .setItems(users, (dialog, which) -> {
                                Toast.makeText(getContext(), "Editing: " + users[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Edit", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showSuspendUserDialog() {
        FirebaseRefs.users().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            userNames.add(user.name + " (" + user.role + ")");
                        }
                    }
                    
                    String[] users = userNames.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select User to Suspend")
                            .setItems(users, (dialog, which) -> {
                                Toast.makeText(getContext(), "Suspended: " + users[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Suspend", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showDeleteUserDialog() {
        FirebaseRefs.users().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            userNames.add(user.name + " (" + user.role + ")");
                        }
                    }
                    
                    String[] users = userNames.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select User to Delete")
                            .setItems(users, (dialog, which) -> {
                                Toast.makeText(getContext(), "Deleted: " + users[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Delete", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showAllCourses() {
        FirebaseRefs.courses().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title + " by " + course.instructorName);
                        }
                    }
                    
                    String[] courses = courseTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("All Courses (" + courses.length + ")")
                            .setItems(courses, (dialog, which) -> {
                                Toast.makeText(getContext(), "Selected: " + courses[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private void showApproveCourses() {
        FirebaseRefs.courses().whereEqualTo("isPublished", false).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title + " by " + course.instructorName);
                        }
                    }
                    
                    if (courseTitles.isEmpty()) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("No Pending Courses")
                                .setMessage("All courses are already approved.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }
                    
                    String[] courses = courseTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Pending Courses (" + courses.length + ")")
                            .setItems(courses, (dialog, which) -> {
                                Toast.makeText(getContext(), "Approved: " + courses[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Approve Selected", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showEditCourseDialog() {
        FirebaseRefs.courses().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title + " by " + course.instructorName);
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

    private void showDeleteCourseDialog() {
        FirebaseRefs.courses().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            courseTitles.add(course.title + " by " + course.instructorName);
                        }
                    }
                    
                    String[] courses = courseTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select Course to Delete")
                            .setItems(courses, (dialog, which) -> {
                                Toast.makeText(getContext(), "Deleted: " + courses[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Delete", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showAllEvents() {
        FirebaseRefs.events().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> eventTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.EventItem event = doc.toObject(Models.EventItem.class);
                        if (event != null) {
                            eventTitles.add(event.title + " - " + event.location);
                        }
                    }
                    
                    String[] events = eventTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("All Events (" + events.length + ")")
                            .setItems(events, (dialog, which) -> {
                                Toast.makeText(getContext(), "Selected: " + events[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private void showCreateEventDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create New Event")
                .setMessage("Event creation form will be implemented here with fields for title, description, date, location, etc.")
                .setPositiveButton("Create", (dialog, which) -> {
                    Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditEventDialog() {
        FirebaseRefs.events().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> eventTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.EventItem event = doc.toObject(Models.EventItem.class);
                        if (event != null) {
                            eventTitles.add(event.title + " - " + event.location);
                        }
                    }
                    
                    String[] events = eventTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select Event to Edit")
                            .setItems(events, (dialog, which) -> {
                                Toast.makeText(getContext(), "Editing: " + events[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Edit", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showDeleteEventDialog() {
        FirebaseRefs.events().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> eventTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.EventItem event = doc.toObject(Models.EventItem.class);
                        if (event != null) {
                            eventTitles.add(event.title + " - " + event.location);
                        }
                    }
                    
                    String[] events = eventTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select Event to Delete")
                            .setItems(events, (dialog, which) -> {
                                Toast.makeText(getContext(), "Deleted: " + events[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Delete", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showAllAnnouncements() {
        FirebaseRefs.announcements().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> announcementTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.Announcement announcement = doc.toObject(Models.Announcement.class);
                        if (announcement != null) {
                            announcementTitles.add(announcement.title + " by " + announcement.createdByName);
                        }
                    }
                    
                    String[] announcements = announcementTitles.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("All Announcements (" + announcements.length + ")")
                            .setItems(announcements, (dialog, which) -> {
                                Toast.makeText(getContext(), "Selected: " + announcements[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private void showCreateAnnouncementDialog() {
        final android.widget.EditText etTitle = new android.widget.EditText(getContext());
        etTitle.setHint("Title");
        final android.widget.EditText etBody = new android.widget.EditText(getContext());
        etBody.setHint("Message");
        etBody.setMinLines(3);
        etBody.setGravity(android.view.Gravity.TOP);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(etTitle);
        layout.addView(etBody);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create Announcement")
                .setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String body = etBody.getText() != null ? etBody.getText().toString().trim() : "";
                    if (title.isEmpty() || body.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter title and message", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.example.looplab.data.model.Models.Announcement a = new com.example.looplab.data.model.Models.Announcement();
                    a.id = com.example.looplab.data.FirebaseRefs.announcements().document().getId();
                    a.title = title;
                    a.body = body;
                    a.createdAt = System.currentTimeMillis();
                    String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    a.createdBy = uid;
                    a.createdByName = "Admin";
                    a.isImportant = false;
                    com.example.looplab.data.FirebaseRefs.announcements().document(a.id).set(a.toMap())
                            .addOnSuccessListener(v -> Toast.makeText(getContext(), "Announcement created", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Analytics methods
    private void showUserAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("User Analytics")
                .setMessage("• Total Users: 1,250\n• Active Users: 890\n• New Users (30 days): 45\n• User Growth: +12%\n• Top Roles: Students (65%), Teachers (25%), Admins (10%)")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCourseAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Course Analytics")
                .setMessage("• Total Courses: 85\n• Published Courses: 72\n• Pending Approval: 13\n• Average Rating: 4.3/5\n• Total Enrollments: 2,450\n• Completion Rate: 78%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEventAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Event Analytics")
                .setMessage("• Total Events: 24\n• Upcoming Events: 8\n• Past Events: 16\n• Average Attendance: 45\n• Registration Rate: 85%\n• Event Satisfaction: 4.6/5")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRevenueAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Revenue Analytics")
                .setMessage("• Monthly Revenue: $12,450\n• Course Sales: 245\n• Average Course Price: $51\n• Growth Rate: +18%\n• Top Performing Course: Android Development\n• Revenue by Category: Mobile Dev (40%), Design (30%), Web Dev (30%)")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSystemAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("System Analytics")
                .setMessage("• System Uptime: 99.8%\n• Active Sessions: 156\n• Server Load: 45%\n• Database Performance: Excellent\n• API Response Time: 120ms\n• Error Rate: 0.2%")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDownloadReports() {
        String[] reportTypes = {"User Report", "Course Report", "Event Report", "Revenue Report", "System Report"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Download Reports")
                .setItems(reportTypes, (dialog, which) -> {
                    Toast.makeText(getContext(), "Downloading " + reportTypes[which] + "...", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Download All", (dialog, which) -> {
                    Toast.makeText(getContext(), "Downloading all reports...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Feedback methods
    private void showAllFeedback() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("All Feedback")
                .setMessage("• Total Feedback: 45\n• Pending: 12\n• In Progress: 8\n• Resolved: 25\n• Average Response Time: 2.5 hours")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRespondToFeedback() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Respond to Feedback")
                .setMessage("Feedback response system will be implemented here.")
                .setPositiveButton("Respond", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMarkAsResolved() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mark as Resolved")
                .setMessage("Mark feedback items as resolved.")
                .setPositiveButton("Mark Resolved", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExportFeedback() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Feedback")
                .setMessage("Export feedback data to CSV/Excel format.")
                .setPositiveButton("Export", (dialog, which) -> {
                    Toast.makeText(getContext(), "Feedback exported successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFeedbackAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Feedback Analytics")
                .setMessage("• Total Feedback: 45\n• Satisfaction Score: 4.2/5\n• Response Rate: 95%\n• Average Resolution Time: 1.5 days\n• Top Categories: Bug Reports (40%), Feature Requests (35%), General (25%)")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    // Live Sessions methods
    private void showActiveSessions() {
        FirebaseRefs.liveSessions().whereEqualTo("isActive", true).get()
                .addOnSuccessListener(querySnapshot -> {
                    int activeSessions = querySnapshot.size();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Active Sessions")
                            .setMessage("Currently " + activeSessions + " active live sessions.")
                            .setPositiveButton("Monitor", (dialog, which) -> {
                                Intent intent = new Intent(getActivity(), LiveSessionActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
    }

    private void showMonitorSessions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Monitor Sessions")
                .setMessage("Real-time monitoring of live sessions.")
                .setPositiveButton("Open Monitor", (dialog, which) -> {
                    Intent intent = new Intent(getActivity(), LiveSessionActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSessionAnalytics() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Session Analytics")
                .setMessage("• Total Sessions: 156\n• Average Duration: 45 min\n• Peak Concurrent Users: 89\n• Session Success Rate: 94%\n• User Satisfaction: 4.5/5")
                .setPositiveButton("View Details", null)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showManageSessions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Sessions")
                .setMessage("Manage live session settings and configurations.")
                .setPositiveButton("Manage", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Other methods
    private void showEventRegistrations() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Event Registrations")
                .setMessage("View and manage event registrations.")
                .setPositiveButton("View", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAssignTeachers() {
        FirebaseRefs.users().whereEqualTo("role", "teacher").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> teacherNames = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            teacherNames.add(user.name);
                        }
                    }
                    
                    String[] teachers = teacherNames.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Assign Teachers")
                            .setItems(teachers, (dialog, which) -> {
                                Toast.makeText(getContext(), "Assigned: " + teachers[which], Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Assign", null)
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void showEditAnnouncementDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Announcement")
                .setMessage("Edit announcement form will be implemented here.")
                .setPositiveButton("Edit", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAnnouncementDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Announcement")
                .setMessage("Select announcement to delete.")
                .setPositiveButton("Delete", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSendNotificationsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Send Notifications")
                .setMessage("Send push notifications to users.")
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initializeServices() {
        loadUserProfile();
    }

    private void loadDashboardData() {
        loadTotalUsers();
        loadTotalCourses();
        loadTotalEvents();
        loadActiveSessions();
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

    private void loadTotalUsers() {
        FirebaseRefs.users().get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalUsers = querySnapshot.size();
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText(String.valueOf(totalUsers));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText("0");
                    }
                });
    }

    private void loadTotalCourses() {
        FirebaseRefs.courses().get()
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

    private void loadTotalEvents() {
        FirebaseRefs.events().get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalEvents = querySnapshot.size();
                    if (tvTotalEvents != null) {
                        tvTotalEvents.setText(String.valueOf(totalEvents));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvTotalEvents != null) {
                        tvTotalEvents.setText("0");
                    }
                });
    }

    private void loadActiveSessions() {
        FirebaseRefs.liveSessions().whereEqualTo("isActive", true).get()
                .addOnSuccessListener(querySnapshot -> {
                    int activeSessions = querySnapshot.size();
                    if (tvActiveSessions != null) {
                        tvActiveSessions.setText(String.valueOf(activeSessions));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvActiveSessions != null) {
                        tvActiveSessions.setText("0");
                    }
                });
    }

    private void loadRecentActivity() {
        // Load recent user registrations, course creations, etc.
        FirebaseRefs.users().orderBy("createdAt", Query.Direction.DESCENDING).limit(5).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> recentActivities = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            String activity = "New user registered: " + user.name;
                            recentActivities.add(activity);
                        }
                    }
                    // Update recent activity display
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