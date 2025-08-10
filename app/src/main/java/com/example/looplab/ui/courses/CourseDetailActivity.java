package com.example.looplab.ui.courses;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
        
        // Initialize FAB as hidden by default - will be shown only for instructors
        fabAddLecture.setVisibility(View.GONE);
        
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

    /**
     * Show the completion button for YouTube videos
     * This button allows students to manually mark videos as complete
     * since we can't automatically detect completion in WebView
     */
    private void showYouTubeCompletionButton() {
        if (currentLecture != null && isYouTubeVideo(currentLecture.videoUrl)) {
            // Only show button if lecture is not already completed
            if (!currentLecture.completed) {
                btnMarkComplete.setVisibility(View.VISIBLE);
                btnMarkComplete.setText("Mark as Complete");
                btnMarkComplete.setEnabled(true);
                
                // Set up click listener for marking completion
                btnMarkComplete.setOnClickListener(v -> markYouTubeVideoAsComplete(v));
            } else {
                // Lecture already completed, hide button
                btnMarkComplete.setVisibility(View.GONE);
            }
        } else {
            // Not a YouTube video or no current lecture
            btnMarkComplete.setVisibility(View.GONE);
        }
    }

    /**
     * Mark the current YouTube video as complete and update course progress
     * This method handles comprehensive progress tracking including:
     * - Lecture completion status
     * - Watch time tracking
     * - Course progress calculation
     * - UI updates
     */
    public void markYouTubeVideoAsComplete(View view) {
        if (currentLecture != null && currentUserId != null) {
            // Show loading state
            btnMarkComplete.setEnabled(false);
            btnMarkComplete.setText("Marking...");
            
            // Mark the current YouTube video as completed with comprehensive progress data
            courseService.updateLectureProgress(currentUserId, courseId, currentLecture.id, currentWatchSeconds, true,
                    new CourseService.ProgressCallback() {
                        @Override
                        public void onSuccess(Models.Progress progress) {
                            // Update local lecture data
                            currentLecture.completed = true;
                            
                            // Update completed lectures count and recalculate progress
                            updateCompletedLecturesCount();
                            updateCourseProgress();
                            
                            // Update UI
                            updateLectureItemUI(currentLecture);
                            lecturesAdapter.notifyDataSetChanged();
                            
                            // Show success message with progress info
                            int progressPercentage = totalLectures > 0 ? (completedLectures * 100) / totalLectures : 0;
                            String message = String.format("Lecture completed! Course progress: %d%% (%d/%d lectures)", 
                                progressPercentage, completedLectures, totalLectures);
                            Toast.makeText(CourseDetailActivity.this, message, Toast.LENGTH_LONG).show();
                            
                            // Hide the mark complete button after successful completion
                            btnMarkComplete.setVisibility(View.GONE);
                            
                            // Check if course is fully completed
                            checkCourseCompletion();
                        }

                        @Override
                        public void onError(String error) {
                            // Restore button state on error
                            btnMarkComplete.setEnabled(true);
                            btnMarkComplete.setText("Mark as Complete");
                            Toast.makeText(CourseDetailActivity.this, "Error marking lecture as complete: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setupRecyclerView() {
        rvLectures.setLayoutManager(new LinearLayoutManager(this));
        lecturesAdapter = new LecturesAdapter(lecture -> playLecture(lecture));
        rvLectures.setAdapter(lecturesAdapter);
        
        // Check if current user is the course instructor and show/hide FAB accordingly
        checkUserPermissionsAndSetupFAB();
    }

    /**
     * Check if the current user has instructor permissions for this course
     * and show/hide the FAB accordingly.
     * - Instructors: FAB is visible and functional
     * - Students: FAB is completely hidden
     */
    private void checkUserPermissionsAndSetupFAB() {
        // Check if current user is the course instructor
        FirebaseRefs.courses().document(courseId).get().addOnSuccessListener(doc -> {
            Models.Course c = doc.toObject(Models.Course.class);
            if (c != null && currentUserId != null && currentUserId.equals(c.instructorId)) {
                // User is the instructor - show FAB and set up click listener
                fabAddLecture.setVisibility(View.VISIBLE);
                fabAddLecture.setOnClickListener(v -> {
                    Intent i = new Intent(this, CreateLectureActivity.class);
                    i.putExtra("course_id", courseId);
                    startActivity(i);
                });
            } else {
                // User is a student - hide FAB completely
                fabAddLecture.setVisibility(View.GONE);
            }
        }).addOnFailureListener(e -> {
            // If there's an error loading course info, hide FAB for safety
            fabAddLecture.setVisibility(View.GONE);
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
                
                // Load user progress after lectures are loaded
                // This will update completion status and progress UI
                loadUserProgress();
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(CourseDetailActivity.this, "Failed to load lectures: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load user's progress for this course from Firebase
     * This includes completed lectures and watch time data
     */
    private void loadUserProgress() {
        if (currentUserId == null) return;
        
        courseService.getUserProgress(currentUserId, courseId, new CourseService.ProgressCallback() {
            @Override
            public void onSuccess(Models.Progress progress) {
                if (progress != null && progress.completedLectures != null) {
                    // Update completed lectures count from Firebase data
                    completedLectures = progress.completedLectures.size();
                    
                    // Update local lecture completion status based on Firebase data
                    updateLocalLectureCompletionStatus(progress.completedLectures);
                    
                    // Update UI
                    updateCourseProgress();
                    lecturesAdapter.notifyDataSetChanged();
                } else {
                    // No progress data found, start fresh
                    completedLectures = 0;
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

    /**
     * Update local lecture completion status based on Firebase progress data
     */
    private void updateLocalLectureCompletionStatus(List<String> completedLectureIds) {
        if (completedLectureIds == null) return;
        
        for (Models.Lecture lecture : allLectures) {
            lecture.completed = completedLectureIds.contains(lecture.id);
        }
    }

    /**
     * Update the course progress UI with current completion status
     */
    private void updateCourseProgress() {
        if (totalLectures > 0) {
            int progress = (completedLectures * 100) / totalLectures;
            courseProgressBar.setProgress(progress);
            tvProgressPercentage.setText(progress + "%");
            
            // Update progress bar color based on completion level
            if (progress >= 100) {
                // Course completed - show success color
                courseProgressBar.setIndicatorColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (progress >= 50) {
                // Good progress - show progress color
                courseProgressBar.setIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                // Early progress - show default color
                courseProgressBar.setIndicatorColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
        }
    }

    /**
     * Update the completed lectures count by counting all completed lectures
     */
    private void updateCompletedLecturesCount() {
        completedLectures = 0;
        for (Models.Lecture lecture : allLectures) {
            if (lecture.completed) {
                completedLectures++;
            }
        }
    }

    /**
     * Update the UI for a specific lecture item to reflect completion status
     */
    private void updateLectureItemUI(Models.Lecture lecture) {
        // Find the lecture in the adapter and update its UI
        int position = -1;
        for (int i = 0; i < allLectures.size(); i++) {
            if (allLectures.get(i).id.equals(lecture.id)) {
                position = i;
                break;
            }
        }
        
        if (position != -1) {
            // Update the lecture in the list
            allLectures.set(position, lecture);
            // Notify the adapter of the specific item change
            lecturesAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Check if the course is fully completed and show appropriate feedback
     */
    private void checkCourseCompletion() {
        if (completedLectures >= totalLectures && totalLectures > 0) {
            // Course is fully completed
            showCourseCompletionCelebration();
        }
    }

    /**
     * Show celebration UI when course is fully completed
     */
    private void showCourseCompletionCelebration() {
        // Show a congratulatory message
        String message = "🎉 Congratulations! You've completed the entire course! 🎉";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // Update the course progress bar to show 100% completion
        courseProgressBar.setProgress(100);
        tvProgressPercentage.setText("100%");
        
        // You could also show a dialog or navigate to a completion screen here
        // For now, we'll just show the toast message
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

    /**
     * Play a lecture video (either local video or YouTube)
     * Resets progress tracking and updates UI accordingly
     */
    private void playLecture(Models.Lecture lecture) {
        if (lecture == null || lecture.videoUrl == null || lecture.videoUrl.isEmpty()) {
            Toast.makeText(this, "Lecture has no video", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Reset progress tracking for new lecture
        resetProgressTracking();
        
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

    /**
     * Reset progress tracking when switching between lectures
     */
    private void resetProgressTracking() {
        // Stop current progress tracking
        progressHandler.removeCallbacksAndMessages(null);
        
        // Reset watch time
        currentWatchSeconds = 0;
        
        // Hide completion button
        btnMarkComplete.setVisibility(View.GONE);
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

    /**
     * Play a YouTube video in the embedded WebView
     * Sets up the video player and shows completion tracking UI
     */
    private void playYouTubeVideo(String videoUrl) {
        try {
            progressIndicator.setVisibility(View.VISIBLE);
            
            // Hide other video elements
            videoView.setVisibility(View.GONE);
            videoPlaceholder.setVisibility(View.GONE);
            
            // Convert YouTube URL to embed format for better WebView compatibility
            String embedUrl = convertToEmbedUrl(videoUrl);
            
            // Load the YouTube embed URL in WebView
            youtubeWebView.loadUrl(embedUrl);
            
            // Note: The completion button will be shown in onPageFinished callback
            // after the video loads successfully
            
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

    /**
     * Start tracking video playback progress
     * Updates progress every second and saves to Firebase every 10 seconds
     */
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
                
                if (isPlaying && currentLecture != null && !currentLecture.completed) {
                    currentWatchSeconds += 1;
                    
                    // Update progress to Firebase every 10 seconds for better performance
                    if (currentUserId != null && currentWatchSeconds % 10 == 0) {
                        courseService.updateLectureProgress(currentUserId, courseId, currentLecture.id, currentWatchSeconds, false,
                                new CourseService.ProgressCallback() {
                                    @Override public void onSuccess(Models.Progress progress) {
                                        // Progress updated successfully
                                    }
                                    @Override public void onError(String error) {
                                        // Log error but continue tracking
                                        Log.w("CourseDetail", "Failed to update progress: " + error);
                                    }
                                });
                    }
                    
                    // Update lecture progress UI if available
                    updateLectureProgressUI(currentWatchSeconds, currentLecture.duration);
                }
                
                progressHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    /**
     * Update the lecture progress UI to show current watch time vs total duration
     */
    private void updateLectureProgressUI(int watchSeconds, int totalDuration) {
        if (totalDuration > 0) {
            int progressPercentage = (watchSeconds * 100) / totalDuration;
            // You can update a progress indicator here if you want to show individual lecture progress
            // For now, we'll just track the time in the background
        }
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


