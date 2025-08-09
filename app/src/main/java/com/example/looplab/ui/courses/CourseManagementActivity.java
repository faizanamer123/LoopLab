package com.example.looplab.ui.courses;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.model.Models;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CourseManagementActivity extends AppCompatActivity {

    private RecyclerView rvCourses;
    private CircularProgressIndicator progressIndicator;
    private ManageCoursesAdapter adapter;
    private CourseService courseService;
    private String currentUserId;
    private String currentUserRole;
    private static final int REQUEST_CREATE_COURSE = 1001;
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
        setupRecyclerView();
        initializeServices();
        loadCourses();
    }

    private void initializeViews() {
        rvCourses = findViewById(R.id.rvCourses);
        progressIndicator = findViewById(R.id.progressIndicator);
        FloatingActionButton fabAddCourse = findViewById(R.id.fabAddCourse);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        fabAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateCourseActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_COURSE);
        });
    }

    private void setupRecyclerView() {
        adapter = new ManageCoursesAdapter(new ManageCoursesAdapter.Listener() {
            @Override public void onTogglePublish(Models.Course c, boolean publish) {
                c.isPublished = publish;
                courseService.updateCourse(c, new CourseService.CourseCallback() {
                    @Override public void onSuccess() { android.widget.Toast.makeText(CourseManagementActivity.this, publish?"Published":"Unpublished", android.widget.Toast.LENGTH_SHORT).show(); }
                    @Override public void onError(String error) { android.widget.Toast.makeText(CourseManagementActivity.this, error, android.widget.Toast.LENGTH_SHORT).show(); }
                });
            }
            @Override public void onPickThumbnail(Models.Course c) {
                pendingThumbnailCourseId = c.id;
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                imagePickerLauncher.launch(android.content.Intent.createChooser(i, "Select Thumbnail"));
            }
            @Override public void onOpenDetails(Models.Course c) {
                android.content.Intent i = new android.content.Intent(CourseManagementActivity.this, CourseDetailActivity.class);
                i.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, c.id);
                startActivity(i);
            }
        });
        
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(adapter);
    }

    private void initializeServices() {
        courseService = new CourseService();
    }

    private void loadCourses() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        if (currentUserId == null) {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load courses based on user role
        courseService.getCoursesByInstructor(currentUserId, new CourseService.CourseListCallback() {
            @Override
            public void onSuccess(List<Models.Course> courses) {
                progressIndicator.setVisibility(View.GONE);
                adapter.submit(courses);
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CourseManagementActivity.this, "Error loading courses: " + error, Toast.LENGTH_SHORT).show();
                // Load sample data if network fails
                loadSampleCourses();
            }
        });
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
                                android.widget.Toast.makeText(this, "Thumbnail updated", android.widget.Toast.LENGTH_SHORT).show();
                                loadCourses();
                            })
                            .addOnFailureListener(e -> {
                                progressIndicator.setVisibility(View.GONE);
                                android.widget.Toast.makeText(this, e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    android.widget.Toast.makeText(this, e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    // Management adapter with publish toggle and thumbnail action
    static class ManageCoursesAdapter extends RecyclerView.Adapter<ManageCoursesAdapter.VH> {
        interface Listener { void onTogglePublish(Models.Course c, boolean publish); void onPickThumbnail(Models.Course c); void onOpenDetails(Models.Course c); }
        private final java.util.List<Models.Course> data = new java.util.ArrayList<>();
        private final Listener listener;
        ManageCoursesAdapter(Listener l) { this.listener = l; }
        void submit(java.util.List<Models.Course> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_course_manage, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final android.widget.ImageView ivThumb; private final android.widget.TextView tvTitle; private final android.widget.TextView tvMeta; private final com.google.android.material.switchmaterial.SwitchMaterial swPublish; private final com.google.android.material.button.MaterialButton btnThumb; private final com.google.android.material.button.MaterialButton btnOpen;
            VH(android.view.View itemView){ super(itemView); ivThumb=itemView.findViewById(R.id.ivThumb); tvTitle=itemView.findViewById(R.id.tvTitle); tvMeta=itemView.findViewById(R.id.tvMeta); swPublish=itemView.findViewById(R.id.swPublish); btnThumb=itemView.findViewById(R.id.btnThumb); btnOpen=itemView.findViewById(R.id.btnOpen); }
            void bind(Models.Course c, Listener l){
                tvTitle.setText(c.title);
                tvMeta.setText("Lectures: " + c.lectureCount + "  Enrolled: " + c.enrolledCount);
                swPublish.setOnCheckedChangeListener(null);
                swPublish.setChecked(c.isPublished);
                swPublish.setOnCheckedChangeListener((buttonView, isChecked) -> l.onTogglePublish(c, isChecked));
                btnThumb.setOnClickListener(v -> l.onPickThumbnail(c));
                btnOpen.setOnClickListener(v -> l.onOpenDetails(c));
                if (c.thumbnailUrl != null && !c.thumbnailUrl.isEmpty()) {
                    Glide.with(itemView.getContext()).load(c.thumbnailUrl).placeholder(R.drawable.ic_courses).into(ivThumb);
                } else {
                    ivThumb.setImageResource(R.drawable.ic_courses);
                }
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

        adapter.submit(sampleCourses);
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
        if (requestCode == REQUEST_CREATE_COURSE && resultCode == RESULT_OK) {
            loadCourses(); // Refresh the course list
        }
    }
}
