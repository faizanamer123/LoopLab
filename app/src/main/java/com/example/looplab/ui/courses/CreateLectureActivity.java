package com.example.looplab.ui.courses;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class CreateLectureActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etVideoUrl, etDuration;
    private MaterialButton btnSave, btnCancel;
    private String courseId;
    private CourseService courseService;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_lecture);

        courseId = getIntent().getStringExtra("course_id");
        courseService = new CourseService();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etVideoUrl = findViewById(R.id.etVideoUrl);
        etDuration = findViewById(R.id.etDuration);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(v -> save());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void save() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String url = etVideoUrl.getText() != null ? etVideoUrl.getText().toString().trim() : "";
        int dur = 0;
        try { dur = Integer.parseInt(etDuration.getText().toString().trim()); } catch (Exception ignored) {}
        if (title.isEmpty() || url.isEmpty()) {
            Toast.makeText(this, "Title and Video URL required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log the video URL for debugging
        android.util.Log.d("CreateLecture", "Saving lecture with video URL: " + url);
        
        Models.Lecture l = new Models.Lecture();
        l.courseId = courseId;
        l.title = title;
        l.description = desc;
        l.videoUrl = url;
        l.duration = dur;
        l.order = (int) (System.currentTimeMillis() / 1000);
        l.isPublished = true;
        
        // Log the lecture object before saving
        android.util.Log.d("CreateLecture", "Lecture object created - isPublished: " + l.isPublished + ", videoUrl: " + l.videoUrl);
        
        courseService.addLecture(l, new CourseService.CourseCallback() {
            @Override public void onSuccess() { 
                android.util.Log.d("CreateLecture", "Lecture saved successfully");
                Toast.makeText(CreateLectureActivity.this, "Lecture added", Toast.LENGTH_SHORT).show(); 
                finish(); 
            }
            @Override public void onError(String error) { 
                android.util.Log.e("CreateLecture", "Error saving lecture: " + error);
                Toast.makeText(CreateLectureActivity.this, error, Toast.LENGTH_SHORT).show(); 
            }
        });
    }
}


