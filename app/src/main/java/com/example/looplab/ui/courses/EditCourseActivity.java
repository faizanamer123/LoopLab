package com.example.looplab.ui.courses;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EditCourseActivity extends AppCompatActivity {

    private TextInputEditText etCourseTitle, etCourseDescription, etLectureCount;
    private AutoCompleteTextView spinnerCategory, spinnerDifficulty;
    private MaterialButton btnUpdateCourse, btnCancel, btnDeleteCourse;
    private CourseService courseService;
    private String currentUserId;
    private String courseId;
    private Models.Course currentCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_course);

        courseId = getIntent().getStringExtra("course_id");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (courseId == null) {
            Toast.makeText(this, "Course ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinners();
        setupClickListeners();
        initializeServices();
        loadCourseData();
    }

    private void initializeViews() {
        etCourseTitle = findViewById(R.id.etCourseTitle);
        etCourseDescription = findViewById(R.id.etCourseDescription);
        etLectureCount = findViewById(R.id.etLectureCount);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        btnUpdateCourse = findViewById(R.id.btnUpdateCourse);
        btnCancel = findViewById(R.id.btnCancel);
        btnDeleteCourse = findViewById(R.id.btnDeleteCourse);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Category spinner
        List<String> categories = new ArrayList<>();
        categories.add("Programming");
        categories.add("Design");
        categories.add("Business");
        categories.add("Marketing");
        categories.add("Data Science");
        categories.add("Mobile Development");
        categories.add("Web Development");
        categories.add("AI & Machine Learning");
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        // Difficulty spinner
        List<String> difficulties = new ArrayList<>();
        difficulties.add("Beginner");
        difficulties.add("Intermediate");
        difficulties.add("Advanced");
        
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, difficulties);
        spinnerDifficulty.setAdapter(difficultyAdapter);
    }

    private void setupClickListeners() {
        btnUpdateCourse.setOnClickListener(v -> {
            if (validateInputs()) {
                updateCourse();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
        
        btnDeleteCourse.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void initializeServices() {
        courseService = new CourseService();
    }

    private void loadCourseData() {
        FirebaseRefs.courses().document(courseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentCourse = documentSnapshot.toObject(Models.Course.class);
                        if (currentCourse != null) {
                            populateFields();
                        }
                    } else {
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        if (currentCourse != null) {
            etCourseTitle.setText(currentCourse.title);
            etCourseDescription.setText(currentCourse.description);
            etLectureCount.setText(String.valueOf(currentCourse.lectureCount));
            
            // Set category and difficulty
            if (currentCourse.category != null) {
                spinnerCategory.setText(currentCourse.category, false);
            }
            if (currentCourse.difficulty != null) {
                spinnerDifficulty.setText(currentCourse.difficulty, false);
            }
        }
    }

    private boolean validateInputs() {
        String title = etCourseTitle.getText().toString().trim();
        String description = etCourseDescription.getText().toString().trim();
        String lectureCountStr = etLectureCount.getText().toString().trim();

        if (title.isEmpty()) {
            etCourseTitle.setError("Course title is required");
            return false;
        }

        if (description.isEmpty()) {
            etCourseDescription.setError("Course description is required");
            return false;
        }

        if (lectureCountStr.isEmpty()) {
            etLectureCount.setError("Number of lectures is required");
            return false;
        }

        try {
            int lectureCount = Integer.parseInt(lectureCountStr);
            if (lectureCount <= 0) {
                etLectureCount.setError("Number of lectures must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etLectureCount.setError("Please enter a valid number");
            return false;
        }

        return true;
    }

    private void updateCourse() {
        String title = etCourseTitle.getText().toString().trim();
        String description = etCourseDescription.getText().toString().trim();
        int lectureCount = Integer.parseInt(etLectureCount.getText().toString().trim());
        String category = spinnerCategory.getText().toString();
        String difficulty = spinnerDifficulty.getText().toString();

        // Update course object
        currentCourse.title = title;
        currentCourse.description = description;
        currentCourse.lectureCount = lectureCount;
        currentCourse.category = category;
        currentCourse.difficulty = difficulty;

        // Show loading state
        btnUpdateCourse.setEnabled(false);
        btnUpdateCourse.setText("Updating...");

        // Update course in Firebase
        courseService.updateCourse(currentCourse, new CourseService.CourseCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditCourseActivity.this, "Course updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditCourseActivity.this, "Error updating course: " + error, Toast.LENGTH_SHORT).show();
                btnUpdateCourse.setEnabled(true);
                btnUpdateCourse.setText("Update Course");
            }
        });
    }

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete '" + currentCourse.title + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse() {
        courseService.deleteCourse(courseId, new CourseService.CourseCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditCourseActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditCourseActivity.this, "Error deleting course: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
