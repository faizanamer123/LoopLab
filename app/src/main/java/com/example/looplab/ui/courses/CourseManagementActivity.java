package com.example.looplab.ui.courses;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.bumptech.glide.Glide;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CourseManagementActivity extends AppCompatActivity {

    private RecyclerView rvCourses;
    private CircularProgressIndicator progressIndicator;
    private RecyclerView.Adapter adapter;
    private CourseService courseService;
    private String currentUserId;
    private String currentUserRole;
    private static final int REQUEST_CREATE_COURSE = 1001;
    private static final int REQUEST_EDIT_COURSE = 1002;
    private String pendingThumbnailCourseId;
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (pendingThumbnailCourseId != null) {
                        uploadThumbnail(pendingThumbnailCourseId, uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_course_management);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        initializeServices();
        determineUserRoleAndLoadCourses();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        rvCourses = findViewById(R.id.rvCourses);
        progressIndicator = findViewById(R.id.progressIndicator);
        FloatingActionButton fabAddCourse = findViewById(R.id.fabAddCourse);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Only show add course button for teachers and admins
        fabAddCourse.setVisibility(View.GONE); // Will be shown later if user is teacher/admin
        
        fabAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateCourseActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_COURSE);
        });
    }

    private void updateUIForUserRole() {
        FloatingActionButton fabAddCourse = findViewById(R.id.fabAddCourse);
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        
        if ("teacher".equals(currentUserRole) || "admin".equals(currentUserRole)) {
            // Show add course button for teachers and admins
            fabAddCourse.setVisibility(View.VISIBLE);
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Manage My Courses");
            }
            if (tvToolbarTitle != null) {
                tvToolbarTitle.setText("Manage My Courses");
            }
        } else {
            // Hide add course button for students
            fabAddCourse.setVisibility(View.GONE);
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("My Enrolled Courses");
            }
            if (tvToolbarTitle != null) {
                tvToolbarTitle.setText("My Enrolled Courses");
            }
        }
    }

    private void setupRecyclerView() {
        // We'll create the appropriate adapter after determining user role
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
    }

    private void createAppropriateAdapter() {
        if ("teacher".equals(currentUserRole) || "admin".equals(currentUserRole)) {
            // Create management adapter for teachers/admins
        adapter = new ManageCoursesAdapter(new ManageCoursesAdapter.Listener() {
            @Override public void onTogglePublish(Models.Course c, boolean publish) {
                c.isPublished = publish;
                courseService.updateCourse(c, new CourseService.CourseCallback() {
                    @Override public void onSuccess() { Toast.makeText(CourseManagementActivity.this, publish?"Published":"Unpublished", Toast.LENGTH_SHORT).show(); }
                    @Override public void onError(String error) { Toast.makeText(CourseManagementActivity.this, error, Toast.LENGTH_SHORT).show(); }
                });
            }
            @Override public void onPickThumbnail(Models.Course c) {
                pendingThumbnailCourseId = c.id;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                imagePickerLauncher.launch(Intent.createChooser(i, "Select Thumbnail"));
            }
            @Override public void onOpenDetails(Models.Course c) {
                Intent i = new Intent(CourseManagementActivity.this, CourseDetailActivity.class);
                i.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, c.id);
                startActivity(i);
            }
            @Override public void onEditCourse(Models.Course c) {
                Intent intent = new Intent(CourseManagementActivity.this, EditCourseActivity.class);
                intent.putExtra("course_id", c.id);
                startActivityForResult(intent, REQUEST_EDIT_COURSE);
            }
        });
        } else {
            // Create enrollment adapter for students
            adapter = new EnrolledCoursesAdapter(new EnrolledCoursesAdapter.Listener() {
                @Override public void onOpenCourse(Models.Course c) {
                    Intent i = new Intent(CourseManagementActivity.this, CourseDetailActivity.class);
                    i.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, c.id);
                    startActivity(i);
                }
                @Override public void onViewProgress(Models.Course c) {
                    // TODO: Navigate to course progress view
                    Toast.makeText(CourseManagementActivity.this, "Progress view coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        rvCourses.setAdapter(adapter);
    }

    private void initializeServices() {
        courseService = new CourseService();
    }

    private void submitDataToAdapter(List<Models.Course> courses) {
        if (adapter instanceof ManageCoursesAdapter) {
            ((ManageCoursesAdapter) adapter).submit(courses);
        } else if (adapter instanceof EnrolledCoursesAdapter) {
            ((EnrolledCoursesAdapter) adapter).submit(courses);
        }
    }

    private void loadCourses() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        if (currentUserId == null) {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("teacher".equals(currentUserRole) || "admin".equals(currentUserRole)) {
            // Load courses that the user is teaching
        courseService.getCoursesByInstructor(currentUserId, new CourseService.CourseListCallback() {
            @Override
            public void onSuccess(List<Models.Course> courses) {
                progressIndicator.setVisibility(View.GONE);
                    submitDataToAdapter(courses);
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CourseManagementActivity.this, "Error loading courses: " + error, Toast.LENGTH_SHORT).show();
                // Load sample data if network fails
                loadSampleCourses();
            }
        });
        } else {
            // Load courses that the user is enrolled in (student role)
            courseService.getUserEnrollments(currentUserId, new CourseService.CourseListCallback() {
                @Override
                public void onSuccess(List<Models.Course> courses) {
                    progressIndicator.setVisibility(View.GONE);
                    submitDataToAdapter(courses);
                }

                @Override
                public void onError(String error) {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(CourseManagementActivity.this, "Error loading enrolled courses: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void uploadThumbnail(String courseId, android.net.Uri uri) {
        progressIndicator.setVisibility(View.VISIBLE);
        com.google.firebase.storage.StorageReference ref = com.example.looplab.data.FirebaseRefs.storage().getReference()
                .child("course_thumbnails/").child(courseId + ".jpg");
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("thumbnailUrl", downloadUri.toString());
                    com.example.looplab.data.FirebaseRefs.courses().document(courseId).update(updates)
                            .addOnSuccessListener(v -> {
                                progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(this, "Thumbnail updated", Toast.LENGTH_SHORT).show();
                                loadCourses();
                            })
                            .addOnFailureListener(e -> {
                                progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void determineUserRoleAndLoadCourses() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check user role from Firestore
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile userProfile = documentSnapshot.toObject(Models.UserProfile.class);
                        if (userProfile != null && userProfile.role != null) {
                            currentUserRole = userProfile.role;
                            loadCourses();
                            updateUIForUserRole(); // Update UI after role is determined
                            createAppropriateAdapter(); // Create adapter after role is determined
                        } else {
                            // Default to student if role is not set
                            currentUserRole = "student";
                            loadCourses();
                            updateUIForUserRole(); // Update UI after role is determined
                            createAppropriateAdapter(); // Create adapter after role is determined
                        }
                    } else {
                        // Default to student if user profile doesn't exist
                        currentUserRole = "student";
                        loadCourses();
                        updateUIForUserRole(); // Update UI after role is determined
                        createAppropriateAdapter(); // Create adapter after role is determined
                    }
                })
                .addOnFailureListener(e -> {
                    // Default to student on error
                    currentUserRole = "student";
                    loadCourses();
                    updateUIForUserRole(); // Update UI after role is determined
                    createAppropriateAdapter(); // Create adapter after role is determined
                });
    }

    // Management adapter with publish toggle and thumbnail action
    static class ManageCoursesAdapter extends RecyclerView.Adapter<ManageCoursesAdapter.VH> {
        interface Listener { 
            void onTogglePublish(Models.Course c, boolean publish); 
            void onPickThumbnail(Models.Course c); 
            void onOpenDetails(Models.Course c);
            void onEditCourse(Models.Course c);
        }
        private final java.util.List<Models.Course> data = new java.util.ArrayList<>();
        private final Listener listener;
        ManageCoursesAdapter(Listener l) { this.listener = l; }
        void submit(java.util.List<Models.Course> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_course_manage, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final android.widget.ImageView ivThumb; 
            private final android.widget.TextView tvTitle; 
            private final android.widget.TextView tvMeta; 
            private final com.google.android.material.switchmaterial.SwitchMaterial swPublish; 
            private final com.google.android.material.button.MaterialButton btnThumb; 
            private final com.google.android.material.button.MaterialButton btnOpen;
            private final com.google.android.material.button.MaterialButton btnEdit;
            
            VH(android.view.View itemView){ 
                super(itemView); 
                ivThumb=itemView.findViewById(R.id.ivThumb); 
                tvTitle=itemView.findViewById(R.id.tvTitle); 
                tvMeta=itemView.findViewById(R.id.tvMeta); 
                swPublish=itemView.findViewById(R.id.swPublish); 
                btnThumb=itemView.findViewById(R.id.btnThumb); 
                btnOpen=itemView.findViewById(R.id.btnOpen);
                btnEdit=itemView.findViewById(R.id.btnEdit);
            }
            
            void bind(Models.Course c, Listener l){
                tvTitle.setText(c.title);
                tvMeta.setText("Lectures: " + c.lectureCount + "  Enrolled: " + c.enrolledCount);
                swPublish.setOnCheckedChangeListener(null);
                swPublish.setChecked(c.isPublished);
                swPublish.setOnCheckedChangeListener((buttonView, isChecked) -> l.onTogglePublish(c, isChecked));
                btnThumb.setOnClickListener(v -> l.onPickThumbnail(c));
                btnOpen.setOnClickListener(v -> l.onOpenDetails(c));
                btnEdit.setOnClickListener(v -> l.onEditCourse(c));
            }
        }
    }

    // Enrolled courses adapter for students
    static class EnrolledCoursesAdapter extends RecyclerView.Adapter<EnrolledCoursesAdapter.VH> {
        interface Listener { 
            void onOpenCourse(Models.Course c);
            void onViewProgress(Models.Course c);
        }
        private final java.util.List<Models.Course> data = new java.util.ArrayList<>();
        private final Listener listener;
        EnrolledCoursesAdapter(Listener l) { this.listener = l; }
        void submit(java.util.List<Models.Course> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_course_enhanced, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final android.widget.TextView tvCourseTitle; 
            private final android.widget.TextView tvInstructor; 
            private final android.widget.TextView tvLectureCount; 
            private final android.widget.TextView tvDuration; 
            private final android.widget.TextView tvDifficulty; 
            private final com.google.android.material.button.MaterialButton btnEnroll;
            private final com.google.android.material.button.MaterialButton btnPreview;
            private final android.widget.LinearLayout progressOverlay;
            private final android.widget.TextView tvProgressPercent;
            private final com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
            
            VH(android.view.View itemView){ 
                super(itemView); 
                tvCourseTitle=itemView.findViewById(R.id.tvCourseTitle); 
                tvInstructor=itemView.findViewById(R.id.tvInstructor); 
                tvLectureCount=itemView.findViewById(R.id.tvLectureCount); 
                tvDuration=itemView.findViewById(R.id.tvDuration); 
                tvDifficulty=itemView.findViewById(R.id.tvDifficulty); 
                btnEnroll=itemView.findViewById(R.id.btnEnroll);
                btnPreview=itemView.findViewById(R.id.btnPreview);
                progressOverlay=itemView.findViewById(R.id.progressOverlay);
                tvProgressPercent=itemView.findViewById(R.id.tvProgressPercent);
                progressBar=itemView.findViewById(R.id.progressBar);
            }
            
            void bind(Models.Course c, Listener l){
                tvCourseTitle.setText(c.title);
                tvInstructor.setText(c.instructorName != null ? "by " + c.instructorName : "by Unknown instructor");
                tvLectureCount.setText(c.lectureCount + " lectures");
                tvDuration.setText("Enrolled: " + c.enrolledCount);
                tvDifficulty.setText("Progress: " + getEnrollmentProgress(c) + "%");
                
                // Show progress overlay for enrolled courses
                progressOverlay.setVisibility(View.VISIBLE);
                int progress = getEnrollmentProgress(c);
                tvProgressPercent.setText(progress + "%");
                progressBar.setProgress(progress);
                
                // Change button text and actions for enrolled courses
                btnEnroll.setText("Open Course");
                btnPreview.setText("View Progress");
                
                btnEnroll.setOnClickListener(v -> l.onOpenCourse(c));
                btnPreview.setOnClickListener(v -> l.onViewProgress(c));
            }
            
            private int getEnrollmentProgress(Models.Course course) {
                // TODO: Get actual progress from enrollment data
                // For now, return a placeholder value
                return 25; // Placeholder progress
            }
        }
    }

    private void loadSampleCourses() {
        List<Models.Course> sampleCourses = new ArrayList<>();
        
        Models.Course course1 = new Models.Course();
        course1.id = "1";
        course1.title = "Introduction to Android Development";
        course1.description = "Learn the basics of Android app development with Java and XML.";
        course1.instructorId = currentUserId;
        course1.instructorName = "John Doe";
        course1.lectureCount = 12;
        course1.enrolledCount = 45;
        course1.category = "Programming";
        course1.difficulty = "Beginner";
        course1.isPublished = true;
        course1.rating = 4.5;
        sampleCourses.add(course1);

        Models.Course course2 = new Models.Course();
        course2.id = "2";
        course2.title = "Advanced UI/UX Design";
        course2.description = "Master modern UI/UX design principles and tools.";
        course2.instructorId = currentUserId;
        course2.instructorName = "John Doe";
        course2.lectureCount = 8;
        course2.enrolledCount = 23;
        course2.category = "Design";
        course2.difficulty = "Intermediate";
        course2.isPublished = false;
        course2.rating = 4.2;
        sampleCourses.add(course2);

        submitDataToAdapter(sampleCourses);
    }

    private void openCourseDetails(Models.Course course) {
        // For now, show a toast since CourseDetailActivity doesn't exist yet
        Toast.makeText(this, "Course Details: " + course.title, Toast.LENGTH_SHORT).show();
    }

    private void editCourse(Models.Course course) {
        // For now, show a toast since EditCourseActivity doesn't exist yet
        Toast.makeText(this, "Edit Course: " + course.title, Toast.LENGTH_SHORT).show();
    }

    private void deleteCourse(Models.Course course) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete '" + course.title + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    courseService.deleteCourse(course.id, new CourseService.CourseCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(CourseManagementActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                            loadCourses(); // Reload the list
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(CourseManagementActivity.this, "Error deleting course: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleCoursePublish(Models.Course course) {
        course.isPublished = !course.isPublished;
        courseService.updateCourse(course, new CourseService.CourseCallback() {
            @Override
            public void onSuccess() {
                String status = course.isPublished ? "published" : "unpublished";
                Toast.makeText(CourseManagementActivity.this, "Course " + status + " successfully", Toast.LENGTH_SHORT).show();
                loadCourses(); // Reload the list
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CourseManagementActivity.this, "Error updating course: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses(); // Reload courses when returning to this activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CREATE_COURSE || requestCode == REQUEST_EDIT_COURSE) && resultCode == RESULT_OK) {
            loadCourses(); // Refresh the course list
        }
    }
}
