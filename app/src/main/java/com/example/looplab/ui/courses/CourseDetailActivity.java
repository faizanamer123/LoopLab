package com.example.looplab.ui.courses;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private String courseId;
    private String currentUserId;
    private CourseService courseService;

    private MaterialToolbar toolbar;
    private RecyclerView rvLectures;
    private CircularProgressIndicator progressIndicator;
    private android.widget.VideoView videoView;
    private TextView tvEmpty;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddLecture;

    private LecturesAdapter lecturesAdapter;
    private final Handler progressHandler = new Handler();
    private int currentWatchSeconds = 0;
    private @Nullable Models.Lecture currentLecture = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_course_detail);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        courseService = new CourseService();

        toolbar = findViewById(R.id.toolbar);
        rvLectures = findViewById(R.id.rvLectures);
        progressIndicator = findViewById(R.id.progressIndicator);
        videoView = findViewById(R.id.videoView);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddLecture = findViewById(R.id.fabAddLecture);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvLectures.setLayoutManager(new LinearLayoutManager(this));
        lecturesAdapter = new LecturesAdapter(lecture -> playLecture(lecture));
        rvLectures.setAdapter(lecturesAdapter);

        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);

        loadCourseHeader();
        loadLectures();

        fabAddLecture.setOnClickListener(v -> {
            // Only for instructors of this course
            FirebaseRefs.courses().document(courseId).get().addOnSuccessListener(doc -> {
                Models.Course c = doc.toObject(Models.Course.class);
                if (c != null && currentUserId != null && currentUserId.equals(c.instructorId)) {
                    android.content.Intent i = new android.content.Intent(this, CreateLectureActivity.class);
                    i.putExtra("course_id", courseId);
                    startActivity(i);
                } else {
                    Toast.makeText(this, "Only the course instructor can add lectures", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadCourseHeader() {
        FirebaseRefs.courses().document(courseId).get().addOnSuccessListener(doc -> {
            Models.Course c = doc.toObject(Models.Course.class);
            if (c != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(c.title);
                getSupportActionBar().setSubtitle(c.instructorName);
                
                // Show course info in the UI
                if (c.description != null && !c.description.isEmpty()) {
                    // You can add a TextView for description if needed
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadLectures() {
        progressIndicator.setVisibility(View.VISIBLE);
        courseService.getCourseLectures(courseId, new CourseService.LectureCallback() {
            @Override
            public void onSuccess(List<Models.Lecture> lectures) {
                progressIndicator.setVisibility(View.GONE);
                if (lectures == null) lectures = new ArrayList<>();
                lecturesAdapter.submit(lectures);
                tvEmpty.setVisibility(lectures.isEmpty() ? View.VISIBLE : View.GONE);
                
                // Show course info even if no lectures
                if (lectures.isEmpty()) {
                    // Load course info to show at least something
                    loadCourseHeader();
                } else {
                    // Auto-play first lecture
                    playLecture(lectures.get(0));
                }
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(CourseDetailActivity.this, "Failed to load lectures: " + error, Toast.LENGTH_SHORT).show();
                
                // Still try to load course info
                loadCourseHeader();
            }
        });
    }

    private void playLecture(Models.Lecture lecture) {
        if (lecture == null || lecture.videoUrl == null || lecture.videoUrl.isEmpty()) {
            Toast.makeText(this, "Lecture has no video", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentLecture = lecture;
        currentWatchSeconds = 0;
        
        // Check if it's a YouTube video
        if (isYouTubeVideo(lecture.videoUrl)) {
            openYouTubeVideo(lecture.videoUrl);
            return;
        }
        
        try {
            Uri videoUri = getVideoUri(lecture.videoUrl);
            
            // Set up video view with better error handling
            videoView.setVideoURI(videoUri);
            
            videoView.setOnPreparedListener(mp -> {
                // Video is ready to play
                videoView.start();
                startProgressLoop();
                
                // Set video view size to maintain aspect ratio
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                if (videoWidth > 0 && videoHeight > 0) {
                    // You can adjust the video view size here if needed
                }
            });
            
            videoView.setOnErrorListener((mp, what, extra) -> {
                String errorMsg = "Video playback error";
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        errorMsg = "Unknown video error";
                        break;
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        errorMsg = "Video server error";
                        break;
                    case MediaPlayer.MEDIA_ERROR_IO:
                        errorMsg = "Video I/O error";
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        errorMsg = "Video format error";
                        break;
                    case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                        errorMsg = "Video not suitable for streaming";
                        break;
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                return true; // Error handled
            });
            
            videoView.setOnCompletionListener(mp -> {
                if (currentUserId != null) {
                    courseService.updateLectureProgress(currentUserId, courseId, lecture.id, currentWatchSeconds, true,
                            new CourseService.ProgressCallback() {
                                @Override
                                public void onSuccess(Models.Progress progress) {}

                                @Override
                                public void onError(String error) {}
                            });
                }
            });
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Convert various video URL formats to proper URIs
     * Supports direct video URLs and other streaming services
     * Note: YouTube videos are handled separately in playLecture method
     */
    private Uri getVideoUri(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            throw new IllegalArgumentException("Video URL cannot be null or empty");
        }
        
        // Handle direct video URLs
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            return Uri.parse(videoUrl);
        }
        
        // Handle local file paths
        if (videoUrl.startsWith("file://")) {
            return Uri.parse(videoUrl);
        }
        
        // Handle content URIs
        if (videoUrl.startsWith("content://")) {
            return Uri.parse(videoUrl);
        }
        
        // Default case - assume it's a direct URL
        return Uri.parse(videoUrl);
    }
    
    /**
     * Extract YouTube video ID from various YouTube URL formats
     */
    private String extractYouTubeVideoId(String youtubeUrl) {
        if (youtubeUrl.contains("youtube.com/watch?v=")) {
            return youtubeUrl.split("v=")[1].split("&")[0];
        } else if (youtubeUrl.contains("youtu.be/")) {
            return youtubeUrl.split("youtu.be/")[1].split("\\?")[0];
        }
        return null;
    }
    
    /**
     * Check if the given URL is a YouTube video
     */
    private boolean isYouTubeVideo(String videoUrl) {
        return videoUrl != null && (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be"));
    }
    
    /**
     * Open YouTube video in YouTube app or browser
     */
    private void openYouTubeVideo(String videoUrl) {
        try {
            // First, try to open in YouTube app
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            intent.setPackage("com.google.android.youtube");
            
            // Check if YouTube app is installed
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Toast.makeText(this, "Opening YouTube video in YouTube app", Toast.LENGTH_SHORT).show();
            } else {
                // If YouTube app is not installed, open in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
                Toast.makeText(this, "Opening YouTube video in browser", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Fallback to browser if there's any error
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
                Toast.makeText(this, "Opening YouTube video in browser", Toast.LENGTH_SHORT).show();
            } catch (Exception browserError) {
                Toast.makeText(this, "Error opening YouTube video: " + browserError.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startProgressLoop() {
        progressHandler.removeCallbacksAndMessages(null);
        progressHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    currentWatchSeconds += 1;
                    if (currentUserId != null && currentLecture != null && currentWatchSeconds % 10 == 0) {
                        courseService.updateLectureProgress(currentUserId, courseId, currentLecture.id, currentWatchSeconds, false,
                                new CourseService.ProgressCallback() {
                                    @Override public void onSuccess(Models.Progress progress) {}
                                    @Override public void onError(String error) {}
                                });
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
    }

    private static class LecturesAdapter extends RecyclerView.Adapter<LecturesAdapter.VH> {
        interface OnLectureClick { void onClick(Models.Lecture lecture); }
        private final List<Models.Lecture> data = new ArrayList<>();
        private final OnLectureClick listener;
        LecturesAdapter(OnLectureClick l) { this.listener = l; }
        void submit(List<Models.Lecture> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_lecture, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final TextView tvTitle; private final TextView tvMeta;
            VH(View itemView) { super(itemView); tvTitle = itemView.findViewById(R.id.tvTitle); tvMeta = itemView.findViewById(R.id.tvMeta); }
            void bind(Models.Lecture l, OnLectureClick listener) {
                tvTitle.setText(l.title);
                String dur = l.duration > 0 ? (l.duration/60)+"m" : "";
                tvMeta.setText(dur);
                itemView.setOnClickListener(v -> listener.onClick(l));
            }
        }
    }
}


