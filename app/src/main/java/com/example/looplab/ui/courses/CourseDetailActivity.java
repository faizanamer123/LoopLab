package com.example.looplab.ui.courses;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ImageView;

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
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
    private VideoView videoView;
    private WebView youtubeWebView;
    private TextView tvEmpty;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddLecture;
    
    // New UI elements
    private View videoPlaceholder;
    private TextView tvCurrentLectureTitle;
    private TextView tvCurrentLectureDuration;
    private com.google.android.material.button.MaterialButton btnMarkComplete;
    private LinearProgressIndicator courseProgressBar;
    private TextView tvProgressPercentage;
    private TextView tvLecturesCount;

    private LecturesAdapter lecturesAdapter;
    private final Handler progressHandler = new Handler();
    private int currentWatchSeconds = 0;
    private @Nullable Models.Lecture currentLecture = null;
    private List<Models.Lecture> allLectures = new ArrayList<>();
    private int totalLectures = 0;
    private int completedLectures = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_course_detail);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        courseService = new CourseService();

        initializeViews();
        setupToolbar();
        setupVideoPlayer();
        setupYouTubeWebView();
        setupRecyclerView();
        
        loadCourseHeader();
        loadLectures();
        loadUserProgress();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        rvLectures = findViewById(R.id.rvLectures);
        progressIndicator = findViewById(R.id.progressIndicator);
        videoView = findViewById(R.id.videoView);
        youtubeWebView = findViewById(R.id.youtubeWebView);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddLecture = findViewById(R.id.fabAddLecture);
        
        // New UI elements
        videoPlaceholder = findViewById(R.id.videoPlaceholder);
        tvCurrentLectureTitle = findViewById(R.id.tvCurrentLectureTitle);
        tvCurrentLectureDuration = findViewById(R.id.tvCurrentLectureDuration);
        btnMarkComplete = findViewById(R.id.btnMarkComplete);
        courseProgressBar = findViewById(R.id.courseProgressBar);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);
        tvLecturesCount = findViewById(R.id.tvLecturesCount);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupVideoPlayer() {
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        
        // Set up video player listeners
        videoView.setOnPreparedListener(mp -> {
            progressIndicator.setVisibility(View.GONE);
            videoPlaceholder.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.start();
            startProgressLoop();
        });
        
        videoView.setOnErrorListener((mp, what, extra) -> {
            progressIndicator.setVisibility(View.GONE);
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
            showVideoPlaceholder();
            return true;
        });
        
        videoView.setOnCompletionListener(mp -> {
            if (currentUserId != null && currentLecture != null) {
                courseService.updateLectureProgress(currentUserId, courseId, currentLecture.id, currentWatchSeconds, true,
                        new CourseService.ProgressCallback() {
                            @Override
                            public void onSuccess(Models.Progress progress) {
                                // Mark lecture as completed
                                currentLecture.completed = true;
                                completedLectures++;
                                updateCourseProgress();
                                lecturesAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String error) {}
                        });
            }
        });
    }

    private void setupYouTubeWebView() {
        youtubeWebView.getSettings().setJavaScriptEnabled(true);
        youtubeWebView.getSettings().setDomStorageEnabled(true);
        youtubeWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        youtubeWebView.getSettings().setAllowFileAccess(true);
        youtubeWebView.getSettings().setAllowContentAccess(true);
        
        // Set WebView client to handle page loads
        youtubeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressIndicator.setVisibility(View.GONE);
                videoPlaceholder.setVisibility(View.GONE);
                youtubeWebView.setVisibility(View.VISIBLE);
                startProgressLoop();
                
                // Show completion button for YouTube videos
                showYouTubeCompletionButton();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(CourseDetailActivity.this, "Error loading YouTube video: " + description, Toast.LENGTH_LONG).show();
                showVideoPlaceholder();
            }
        });
        
        // Set WebChromeClient for video progress tracking
        youtubeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressIndicator.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showYouTubeCompletionButton() {
        // Show completion button for YouTube videos since we can't auto-detect completion
        if (currentLecture != null && isYouTubeVideo(currentLecture.videoUrl)) {
            btnMarkComplete.setVisibility(View.VISIBLE);
            btnMarkComplete.setOnClickListener(v -> markYouTubeVideoAsComplete(v));
        } else {
            btnMarkComplete.setVisibility(View.GONE);
        }
    }

    public void markYouTubeVideoAsComplete(View view) {
        if (currentLecture != null && currentUserId != null) {
            // Mark the current YouTube video as completed
            courseService.updateLectureProgress(currentUserId, courseId, currentLecture.id, currentWatchSeconds, true,
                    new CourseService.ProgressCallback() {
                        @Override
                        public void onSuccess(Models.Progress progress) {
                            // Mark lecture as completed
                            currentLecture.completed = true;
                            completedLectures++;
                            updateCourseProgress();
                            lecturesAdapter.notifyDataSetChanged();
                            
                            Toast.makeText(CourseDetailActivity.this, "Lecture marked as completed!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(CourseDetailActivity.this, "Error marking lecture as complete: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setupRecyclerView() {
        rvLectures.setLayoutManager(new LinearLayoutManager(this));
        lecturesAdapter = new LecturesAdapter(lecture -> playLecture(lecture));
        rvLectures.setAdapter(lecturesAdapter);
        
        // Set up FAB click listener
        fabAddLecture.setOnClickListener(v -> {
            // Only for instructors of this course
            FirebaseRefs.courses().document(courseId).get().addOnSuccessListener(doc -> {
                Models.Course c = doc.toObject(Models.Course.class);
                if (c != null && currentUserId != null && currentUserId.equals(c.instructorId)) {
                    Intent i = new Intent(this, CreateLectureActivity.class);
                    i.putExtra("course_id", courseId);
                    startActivity(i);
                } else {
                    Toast.makeText(this, "Only the course instructor can add lectures", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showVideoPlaceholder() {
        videoView.setVisibility(View.GONE);
        youtubeWebView.setVisibility(View.GONE);
        videoPlaceholder.setVisibility(View.VISIBLE);
        progressIndicator.setVisibility(View.GONE);
        btnMarkComplete.setVisibility(View.GONE);
    }

    private void loadCourseHeader() {
        // Load course information for the toolbar
        courseService.getCourse(courseId, new CourseService.SingleCourseCallback() {
            @Override
            public void onSuccess(Models.Course course) {
                // Course loaded successfully
                // You can use the course object here if needed
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CourseDetailActivity.this, "Failed to load course: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLectures() {
        progressIndicator.setVisibility(View.VISIBLE);
        courseService.getCourseLectures(courseId, new CourseService.LectureCallback() {
            @Override
            public void onSuccess(List<Models.Lecture> lectures) {
                progressIndicator.setVisibility(View.GONE);
                if (lectures == null) lectures = new ArrayList<>();
                
                allLectures = lectures;
                totalLectures = lectures.size();
                
                lecturesAdapter.submit(lectures);
                tvEmpty.setVisibility(lectures.isEmpty() ? View.VISIBLE : View.GONE);
                tvLecturesCount.setText(totalLectures + " lecture" + (totalLectures != 1 ? "s" : ""));
                
                if (!lectures.isEmpty()) {
                    // Show first lecture info but don't auto-play
                    updateCurrentLectureInfo(lectures.get(0));
                }
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(CourseDetailActivity.this, "Failed to load lectures: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserProgress() {
        if (currentUserId == null) return;
        
        courseService.getUserProgress(currentUserId, courseId, new CourseService.ProgressCallback() {
            @Override
            public void onSuccess(Models.Progress progress) {
                if (progress != null) {
                    completedLectures = progress.completedLectures != null ? progress.completedLectures.size() : 0;
                    updateCourseProgress();
                }
            }

            @Override
            public void onError(String error) {
                // Progress not found, start fresh
                completedLectures = 0;
                updateCourseProgress();
            }
        });
    }

    private void updateCourseProgress() {
        if (totalLectures > 0) {
            int progress = (completedLectures * 100) / totalLectures;
            courseProgressBar.setProgress(progress);
            tvProgressPercentage.setText(progress + "%");
        }
    }

    private void updateCurrentLectureInfo(Models.Lecture lecture) {
        if (lecture != null) {
            tvCurrentLectureTitle.setText(lecture.title);
            if (lecture.duration > 0) {
                int minutes = lecture.duration / 60;
                int seconds = lecture.duration % 60;
                tvCurrentLectureDuration.setText(String.format("%dm %ds", minutes, seconds));
            } else {
                tvCurrentLectureDuration.setText("");
            }
        }
    }

    private void playLecture(Models.Lecture lecture) {
        if (lecture == null || lecture.videoUrl == null || lecture.videoUrl.isEmpty()) {
            Toast.makeText(this, "Lecture has no video", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentLecture = lecture;
        currentWatchSeconds = 0;
        
        // Update UI
        updateCurrentLectureInfo(lecture);
        showVideoPlaceholder(); // Show placeholder while loading
        
        // Check if it's a YouTube video - handle within WebView
        if (isYouTubeVideo(lecture.videoUrl)) {
            playYouTubeVideo(lecture.videoUrl);
            return;
        }
        
        try {
            Uri videoUri = getVideoUri(lecture.videoUrl);
            progressIndicator.setVisibility(View.VISIBLE);
            btnMarkComplete.setVisibility(View.GONE); // Hide for regular videos
            videoView.setVideoURI(videoUri);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_LONG).show();
            showVideoPlaceholder();
        }
    }
    
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
    
    private boolean isYouTubeVideo(String videoUrl) {
        return videoUrl != null && (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be"));
    }
    
    private void openYouTubeVideo(String videoUrl) {
        try {
            // First, try to open in YouTube app
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            intent.setPackage("com.google.android.youtube");
            
            // Check if YouTube app is installed
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // If YouTube app is not installed, open in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            // Fallback to browser if there's any error
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
            } catch (Exception browserError) {
                Toast.makeText(this, "Error opening YouTube video: " + browserError.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void playYouTubeVideo(String videoUrl) {
        try {
            progressIndicator.setVisibility(View.VISIBLE);
            
            // Hide other video elements
            videoView.setVisibility(View.GONE);
            videoPlaceholder.setVisibility(View.GONE);
            btnMarkComplete.setVisibility(View.GONE); // Will be shown when video loads
            
            // Convert YouTube URL to embed format for better WebView compatibility
            String embedUrl = convertToEmbedUrl(videoUrl);
            
            // Load the YouTube embed URL in WebView
            youtubeWebView.loadUrl(embedUrl);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading YouTube video: " + e.getMessage(), Toast.LENGTH_LONG).show();
            showVideoPlaceholder();
        }
    }

    private String convertToEmbedUrl(String youtubeUrl) {
        String videoId = extractVideoId(youtubeUrl);
        if (videoId != null) {
            return "https://www.youtube.com/embed/" + videoId + "?autoplay=1&rel=0&showinfo=0&modestbranding=1&playsinline=1";
        }
        return youtubeUrl; // Fallback to original URL if extraction fails
    }

    private String extractVideoId(String youtubeUrl) {
        if (youtubeUrl == null) return null;
        
        // Handle different YouTube URL formats
        if (youtubeUrl.contains("youtube.com/watch?v=")) {
            int startIndex = youtubeUrl.indexOf("v=") + 2;
            int endIndex = youtubeUrl.indexOf("&", startIndex);
            if (endIndex == -1) endIndex = youtubeUrl.length();
            return youtubeUrl.substring(startIndex, endIndex);
        } else if (youtubeUrl.contains("youtu.be/")) {
            int startIndex = youtubeUrl.indexOf("youtu.be/") + 9;
            int endIndex = youtubeUrl.indexOf("?", startIndex);
            if (endIndex == -1) endIndex = youtubeUrl.length();
            return youtubeUrl.substring(startIndex, endIndex);
        }
        
        return null;
    }

    private void startProgressLoop() {
        progressHandler.removeCallbacksAndMessages(null);
        progressHandler.postDelayed(new Runnable() {
            @Override public void run() {
                // Track progress for both VideoView and YouTube WebView
                boolean isPlaying = false;
                if (videoView != null && videoView.isPlaying()) {
                    isPlaying = true;
                } else if (youtubeWebView != null && youtubeWebView.getVisibility() == View.VISIBLE) {
                    // For YouTube videos, assume they're playing if WebView is visible
                    // Note: We can't directly detect if YouTube video is playing in WebView
                    isPlaying = true;
                }
                
                if (isPlaying) {
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
        // Pause YouTube video if playing (WebView will handle this automatically)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        if (youtubeWebView != null) {
            youtubeWebView.destroy();
        }
    }

    private static class LecturesAdapter extends RecyclerView.Adapter<LecturesAdapter.VH> {
        interface OnLectureClick { void onClick(Models.Lecture lecture); }
        private final List<Models.Lecture> data = new ArrayList<>();
        private final OnLectureClick listener;
        
        LecturesAdapter(OnLectureClick l) { this.listener = l; }
        
        void submit(List<Models.Lecture> list) { 
            data.clear(); 
            if (list != null) data.addAll(list); 
            notifyDataSetChanged(); 
        }
        
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { 
            return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_lecture, p, false)); 
        }
        
        @Override public void onBindViewHolder(VH h, int i) { 
            h.bind(data.get(i), listener); 
        }
        
        @Override public int getItemCount() { return data.size(); }
        
        static class VH extends RecyclerView.ViewHolder {
            private final TextView tvTitle, tvMeta;
            private final ImageView ivPlayButton, ivCompleted, ivStatus;
            private final View progressContainer;
            private final LinearProgressIndicator lectureProgressBar;
            private final TextView tvProgressText;
            
            VH(View itemView) { 
                super(itemView); 
                tvTitle = itemView.findViewById(R.id.tvTitle); 
                tvMeta = itemView.findViewById(R.id.tvMeta);
                ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
                ivCompleted = itemView.findViewById(R.id.ivCompleted);
                ivStatus = itemView.findViewById(R.id.ivStatus);
                progressContainer = itemView.findViewById(R.id.progressContainer);
                lectureProgressBar = itemView.findViewById(R.id.lectureProgressBar);
                tvProgressText = itemView.findViewById(R.id.tvProgressText);
            }
            
            void bind(Models.Lecture l, OnLectureClick listener) {
                tvTitle.setText(l.title);
                String dur = l.duration > 0 ? (l.duration/60)+"m" : "";
                tvMeta.setText(dur);
                
                // Handle lecture status
                if (l.completed) {
                    ivPlayButton.setVisibility(View.GONE);
                    ivCompleted.setVisibility(View.VISIBLE);
                    progressContainer.setVisibility(View.GONE);
                } else {
                    ivPlayButton.setVisibility(View.VISIBLE);
                    ivCompleted.setVisibility(View.GONE);
                    progressContainer.setVisibility(View.GONE);
                }
                
                itemView.setOnClickListener(v -> listener.onClick(l));
            }
        }
    }
}


