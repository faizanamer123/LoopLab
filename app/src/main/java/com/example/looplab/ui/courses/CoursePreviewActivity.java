package com.example.looplab.ui.courses;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CoursePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private String courseId;
    private String currentUserId;
    private CourseService courseService;

    private MaterialToolbar toolbar;
    private MaterialCardView courseInfoCard;
    private TextView tvCourseTitle, tvCourseDescription, tvInstructorName, tvLectureCount, tvEnrolledCount;
    private TextView tvRating, tvProgressPercent;
    private Chip chipCategory, chipDifficulty;
    private RecyclerView rvPreviewLectures;
    private CircularProgressIndicator progressIndicator;
    private com.google.android.material.progressindicator.LinearProgressIndicator courseProgressBar;
    private MaterialButton btnEnroll, btnBackToCourses;
    private View emptyState;

    private PreviewLecturesAdapter previewLecturesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_preview);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        if (courseId == null || courseId.isEmpty()) {
            Log.e("CoursePreview", "No course ID provided in intent");
            showError("No course ID provided");
            return;
        }
        
        Log.d("CoursePreview", "Course ID received: " + courseId);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        courseService = new CourseService();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadCourseData();
        checkEnrollmentStatus();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        courseInfoCard = findViewById(R.id.courseInfoCard);
        tvCourseTitle = findViewById(R.id.tvCourseTitle);
        tvCourseDescription = findViewById(R.id.tvCourseDescription);
        tvInstructorName = findViewById(R.id.tvInstructorName);
        tvLectureCount = findViewById(R.id.tvLectureCount);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);
        tvRating = findViewById(R.id.tvRating);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        chipCategory = findViewById(R.id.chipCategory);
        chipDifficulty = findViewById(R.id.chipDifficulty);
        rvPreviewLectures = findViewById(R.id.rvPreviewLectures);
        progressIndicator = findViewById(R.id.progressIndicator);
        courseProgressBar = findViewById(R.id.courseProgressBar);
        btnEnroll = findViewById(R.id.btnEnroll);
        btnBackToCourses = findViewById(R.id.btnBackToCourses);
        emptyState = findViewById(R.id.emptyState);

        btnEnroll.setOnClickListener(v -> handleEnroll());
        btnBackToCourses.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvPreviewLectures.setLayoutManager(new LinearLayoutManager(this));
        previewLecturesAdapter = new PreviewLecturesAdapter();
        rvPreviewLectures.setAdapter(previewLecturesAdapter);
    }

    private void loadCourseData() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        // Debug logging
        Log.d("CoursePreview", "Loading course with ID: " + courseId);
        
        FirebaseRefs.courses().document(courseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressIndicator.setVisibility(View.GONE);
                    Log.d("CoursePreview", "Course document exists: " + documentSnapshot.exists());
                    
                    if (documentSnapshot.exists()) {
                        Models.Course course = documentSnapshot.toObject(Models.Course.class);
                        Log.d("CoursePreview", "Course object created: " + (course != null));
                        
                        if (course != null) {
                            Log.d("CoursePreview", "Course title: " + course.title);
                            Log.d("CoursePreview", "Course description: " + course.description);
                            displayCourseInfo(course);
                            loadPreviewLectures();
                        } else {
                            Log.e("CoursePreview", "Failed to create Course object from document");
                            showError("Failed to parse course data");
                        }
                    } else {
                        Log.e("CoursePreview", "Course document does not exist");
                        showError("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Log.e("CoursePreview", "Error loading course: " + e.getMessage());
                    showError("Failed to load course: " + e.getMessage());
                });
    }

    private void displayCourseInfo(Models.Course course) {
        Log.d("CoursePreview", "Displaying course info - Title: " + course.title + 
              ", Description: " + course.description + ", Instructor: " + course.instructorName +
              ", Lecture Count: " + course.lectureCount + ", Enrolled Count: " + course.enrolledCount);
        
        tvCourseTitle.setText(course.title);
        tvCourseDescription.setText(course.description != null ? course.description : "No description available");
        
        // Handle instructor name display
        if (course.instructorName != null && !course.instructorName.trim().isEmpty()) {
            tvInstructorName.setText(course.instructorName);
        } else if (course.instructorId != null && !course.instructorId.isEmpty()) {
            // Try to fetch instructor name from user profile
            fetchInstructorName(course.instructorId);
        } else {
            tvInstructorName.setText("Unknown Instructor");
        }
        
        tvLectureCount.setText(course.lectureCount + " lectures");
        tvEnrolledCount.setText(course.enrolledCount + " students enrolled");
        
        if (course.category != null && !course.category.isEmpty()) {
            chipCategory.setVisibility(View.VISIBLE);
            chipCategory.setText(course.category);
        } else {
            chipCategory.setVisibility(View.GONE);
        }
        
        if (course.difficulty != null && !course.difficulty.isEmpty()) {
            chipDifficulty.setVisibility(View.VISIBLE);
            chipDifficulty.setText(course.difficulty);
        } else {
            chipDifficulty.setVisibility(View.GONE);
        }
        
        if (course.rating > 0) {
            tvRating.setText(String.format("%.1f", course.rating) + " ★");
            tvRating.setVisibility(View.VISIBLE);
        } else {
            tvRating.setVisibility(View.GONE);
        }
        
        // Display course progress
        if (currentUserId != null && course.enrolledCount > 0) {
            // TODO: Get actual progress from enrollment data
            // For now, show a placeholder progress
            int progress = calculateCourseProgress(course);
            tvProgressPercent.setText(progress + "%");
            courseProgressBar.setProgress(progress);
        } else {
            // Show 0% progress for non-enrolled users
            tvProgressPercent.setText("0%");
            courseProgressBar.setProgress(0);
        }
        
        // Ensure progress bar is visible
        courseProgressBar.setVisibility(View.VISIBLE);
        tvProgressPercent.setVisibility(View.VISIBLE);
    }
    
    private int calculateCourseProgress(Models.Course course) {
        // TODO: Implement actual progress calculation based on completed lectures
        // For now, return a placeholder value
        if (course.lectureCount > 0) {
            // Simulate progress based on lecture count
            return Math.min(75, (course.lectureCount * 10)); // Placeholder logic
        }
        return 0;
    }

    private void fetchInstructorName(String instructorId) {
        FirebaseRefs.users().document(instructorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile userProfile = documentSnapshot.toObject(Models.UserProfile.class);
                        if (userProfile != null && userProfile.name != null && !userProfile.name.trim().isEmpty()) {
                            tvInstructorName.setText(userProfile.name);
                            Log.d("CoursePreview", "Fetched instructor name: " + userProfile.name);
                        } else {
                            tvInstructorName.setText("Unknown Instructor");
                            Log.d("CoursePreview", "Instructor profile exists but name is empty");
                        }
                    } else {
                        tvInstructorName.setText("Unknown Instructor");
                        Log.d("CoursePreview", "Instructor profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    tvInstructorName.setText("Unknown Instructor");
                    Log.e("CoursePreview", "Error fetching instructor name: " + e.getMessage());
                });
    }

    private void loadPreviewLectures() {
        Log.d("CoursePreview", "Loading preview lectures for course: " + courseId);
        courseService.getCourseLectures(courseId, new CourseService.LectureCallback() {
            @Override
            public void onSuccess(List<Models.Lecture> lectures) {
                Log.d("CoursePreview", "Received " + lectures.size() + " lectures");
                for (Models.Lecture lecture : lectures) {
                    Log.d("CoursePreview", "Lecture: " + lecture.title + " - Video URL: " + lecture.videoUrl);
                }
                
                if (lectures != null && !lectures.isEmpty()) {
                    // Show only first 3 lectures as preview
                    List<Models.Lecture> previewLectures = lectures.subList(0, Math.min(3, lectures.size()));
                    previewLecturesAdapter.submitLectures(previewLectures);
                    rvPreviewLectures.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                } else {
                    rvPreviewLectures.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("CoursePreview", "Error loading lectures: " + error);
                rvPreviewLectures.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkEnrollmentStatus() {
        if (currentUserId == null) {
            btnEnroll.setText("Sign In to Enroll");
            btnEnroll.setEnabled(false);
            return;
        }

        FirebaseRefs.enrollments().whereEqualTo("userId", currentUserId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        btnEnroll.setText("Already Enrolled");
                        btnEnroll.setEnabled(false);
                        btnEnroll.setBackgroundTintList(getColorStateList(R.color.success));
                    } else {
                        btnEnroll.setText("Enroll Now");
                        btnEnroll.setEnabled(true);
                        btnEnroll.setBackgroundTintList(getColorStateList(R.color.primary));
                    }
                })
                .addOnFailureListener(e -> {
                    btnEnroll.setText("Enroll Now");
                    btnEnroll.setEnabled(true);
                });
    }

    private void handleEnroll() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please sign in to enroll", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnroll.setEnabled(false);
        btnEnroll.setText("Enrolling...");

        courseService.enrollInCourse(currentUserId, courseId, new CourseService.EnrollmentCallback() {
            @Override
            public void onSuccess(Models.Enrollment enrollment) {
                Toast.makeText(CoursePreviewActivity.this, "Successfully enrolled!", Toast.LENGTH_SHORT).show();
                btnEnroll.setText("Enrolled!");
                btnEnroll.setEnabled(false);
                btnEnroll.setBackgroundTintList(getColorStateList(R.color.success));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CoursePreviewActivity.this, "Failed to enroll: " + error, Toast.LENGTH_SHORT).show();
                btnEnroll.setEnabled(true);
                btnEnroll.setText("Enroll Now");
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    // Preview Lectures Adapter
    private static class PreviewLecturesAdapter extends RecyclerView.Adapter<PreviewLecturesAdapter.VH> {
        private final List<Models.Lecture> lectures = new ArrayList<>();

        void submitLectures(List<Models.Lecture> newLectures) {
            android.util.Log.d("PreviewAdapter", "Submitting " + (newLectures != null ? newLectures.size() : 0) + " lectures");
            lectures.clear();
            if (newLectures != null) {
                lectures.addAll(newLectures);
                for (Models.Lecture lecture : newLectures) {
                    android.util.Log.d("PreviewAdapter", "Added lecture: " + lecture.title + " with video URL: " + lecture.videoUrl);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lecture_preview, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            holder.bind(lectures.get(position));
        }

        @Override
        public int getItemCount() {
            return lectures.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            private final TextView tvTitle, tvDuration, tvOrder;

            VH(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvOrder = itemView.findViewById(R.id.tvOrder);
            }

            void bind(Models.Lecture lecture) {
                android.util.Log.d("PreviewAdapter", "Binding lecture - Title: " + lecture.title + 
                                  ", Video URL: " + lecture.videoUrl + ", Duration: " + lecture.duration);
                
                tvTitle.setText(lecture.title);
                tvOrder.setText("Lecture " + lecture.order);
                
                if (lecture.duration > 0) {
                    int minutes = lecture.duration / 60;
                    int seconds = lecture.duration % 60;
                    tvDuration.setText(String.format("%d:%02d", minutes, seconds));
                } else {
                    tvDuration.setText("--:--");
                }
            }
        }
    }
}
