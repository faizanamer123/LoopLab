package com.example.looplab.data;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.ArrayList;

/**
 * Utility class for course progress calculations and UI updates
 * Provides consistent progress tracking across the entire app
 */
public class CourseProgressUtil {
    
    private static final String TAG = "CourseProgressUtil";
    
    /**
     * Calculate course progress based on completed lectures
     * @param completedLectures List of completed lecture IDs
     * @param totalLectures Total number of lectures in the course
     * @return Progress percentage (0-100)
     */
    public static int calculateCourseProgress(List<String> completedLectures, int totalLectures) {
        if (totalLectures <= 0) return 0;
        if (completedLectures == null) return 0;
        
        int completedCount = completedLectures.size();
        return Math.min(100, (completedCount * 100) / totalLectures);
    }
    
    /**
     * Calculate course progress based on completed lectures count
     * @param completedCount Number of completed lectures
     * @param totalLectures Total number of lectures in the course
     * @return Progress percentage (0-100)
     */
    public static int calculateCourseProgress(int completedCount, int totalLectures) {
        if (totalLectures <= 0) return 0;
        return Math.min(100, (completedCount * 100) / totalLectures);
    }
    
    /**
     * Update progress bar UI with progress and appropriate colors
     * @param progressBar The progress bar to update
     * @param progress Progress percentage (0-100)
     * @param context Context for color resources
     */
    public static void updateProgressBarUI(LinearProgressIndicator progressBar, int progress, Context context) {
        if (progressBar == null) return;
        
        progressBar.setProgress(progress);
        
        // Update progress bar color based on completion level
        if (progress >= 100) {
            // Course completed - show success color
            progressBar.setIndicatorColor(Color.parseColor("#4CAF50")); // Green
        } else if (progress >= 75) {
            // Almost complete - show progress color
            progressBar.setIndicatorColor(Color.parseColor("#2196F3")); // Blue
        } else if (progress >= 50) {
            // Good progress - show info color
            progressBar.setIndicatorColor(Color.parseColor("#00BCD4")); // Cyan
        } else if (progress >= 25) {
            // Some progress - show warning color
            progressBar.setIndicatorColor(Color.parseColor("#FF9800")); // Orange
        } else {
            // Early progress - show default color
            progressBar.setIndicatorColor(Color.parseColor("#FF5722")); // Red
        }
    }
    
    /**
     * Get progress text with completion count
     * @param progress Progress percentage
     * @param completedCount Number of completed lectures
     * @param totalLectures Total number of lectures
     * @return Formatted progress text
     */
    public static String getProgressText(int progress, int completedCount, int totalLectures) {
        if (totalLectures <= 0) return "0%";
        return String.format("%d%% (%d/%d)", progress, completedCount, totalLectures);
    }
    
    /**
     * Get progress text with just percentage
     * @param progress Progress percentage
     * @return Formatted progress text
     */
    public static String getProgressText(int progress) {
        return progress + "%";
    }
    
    /**
     * Get motivational message based on progress
     * @param progress Progress percentage
     * @return Motivational message
     */
    public static String getMotivationalMessage(int progress) {
        if (progress >= 100) {
            return "🎉 Course completed! Amazing work!";
        } else if (progress >= 75) {
            return "Almost there! Keep going!";
        } else if (progress >= 50) {
            return "Great progress! You're halfway there!";
        } else if (progress >= 25) {
            return "Good start! Keep learning!";
        } else {
            return "Begin your learning journey!";
        }
    }
    
    /**
     * Load course progress for a specific user and course
     * @param userId User ID
     * @param courseId Course ID
     * @param callback Callback with progress data
     */
    public static void loadCourseProgress(String userId, String courseId, ProgressCallback callback) {
        if (userId == null || courseId == null) {
            callback.onError("Invalid user or course ID");
            return;
        }
        
        FirebaseFirestore.getInstance()
                .collection("progress")
                .whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Models.Progress progress = querySnapshot.getDocuments().get(0).toObject(Models.Progress.class);
                        if (progress != null) {
                            callback.onSuccess(progress);
                        } else {
                            callback.onError("Failed to parse progress data");
                        }
                    } else {
                        // No progress found, create default
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading course progress", e);
                    callback.onError("Failed to load progress: " + e.getMessage());
                });
    }
    
    /**
     * Load all course progress for a user
     * @param userId User ID
     * @param callback Callback with list of progress data
     */
    public static void loadAllUserProgress(String userId, AllProgressCallback callback) {
        if (userId == null) {
            callback.onError("Invalid user ID");
            return;
        }
        
        FirebaseFirestore.getInstance()
                .collection("progress")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Progress> progressList = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Progress progress = doc.toObject(Models.Progress.class);
                        if (progress != null) {
                            progressList.add(progress);
                        }
                    }
                    callback.onSuccess(progressList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all user progress", e);
                    callback.onError("Failed to load progress: " + e.getMessage());
                });
    }
    
    /**
     * Calculate overall learning statistics for a user
     * @param progressList List of user's progress data
     * @return Learning statistics
     */
    public static LearningStats calculateLearningStats(List<Models.Progress> progressList) {
        LearningStats stats = new LearningStats();
        
        if (progressList == null) return stats;
        
        for (Models.Progress progress : progressList) {
            if (progress.completed) {
                stats.completedLectures++;
            }
            if (progress.completedLectures != null) {
                stats.totalCompletedLectures += progress.completedLectures.size();
            }
            stats.totalWatchTime += progress.watchTime;
        }
        
        return stats;
    }
    
    /**
     * Callback interface for progress loading
     */
    public interface ProgressCallback {
        void onSuccess(Models.Progress progress);
        void onError(String error);
    }
    
    /**
     * Callback interface for loading all progress
     */
    public interface AllProgressCallback {
        void onSuccess(List<Models.Progress> progressList);
        void onError(String error);
    }
    
    /**
     * Data class for learning statistics
     */
    public static class LearningStats {
        public int completedLectures = 0;
        public int totalCompletedLectures = 0;
        public long totalWatchTime = 0;
        
        public int getCompletionRate() {
            return completedLectures > 0 ? (totalCompletedLectures * 100) / completedLectures : 0;
        }
    }
}
