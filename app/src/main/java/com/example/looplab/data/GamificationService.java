package com.example.looplab.data;

import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamificationService {
    private static final String TAG = "GamificationService";
    
    public interface GamificationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface LeaderboardCallback {
        void onSuccess(List<Models.LeaderboardEntry> entries);
        void onError(String error);
    }
    
    public interface BadgeCallback {
        void onSuccess(List<Models.Badge> badges);
        void onError(String error);
    }
    
    // Award points to user
    public void awardPoints(String userId, int points, String reason, GamificationCallback callback) {
        DocumentReference userRef = FirebaseRefs.users().document(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("points", FieldValue.increment(points));
        updates.put("lastActive", System.currentTimeMillis());
        
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Awarded " + points + " points to user " + userId + " for: " + reason);
                    updateLeaderboard(userId);
                    checkForBadges(userId, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error awarding points", e);
                    callback.onError("Failed to award points: " + e.getMessage());
                });
    }
    
    // Check and award badges based on user activity
    private void checkForBadges(String userId, GamificationCallback callback) {
        FirebaseRefs.users().document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            checkBadgeCriteria(user, callback);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking badges", e);
                    callback.onError("Failed to check badges: " + e.getMessage());
                });
    }
    
    private void checkBadgeCriteria(Models.UserProfile user, GamificationCallback callback) {
        List<String> newBadges = new ArrayList<>();
        
        // Check for various badge criteria
        if (user.points >= 100 && !hasBadge(user, "first_100")) {
            newBadges.add("first_100");
        }
        if (user.points >= 500 && !hasBadge(user, "point_collector")) {
            newBadges.add("point_collector");
        }
        if (user.points >= 1000 && !hasBadge(user, "point_master")) {
            newBadges.add("point_master");
        }
        
        // Check course completion badges
        checkCourseBadges(user, newBadges);
        
        // Award new badges
        if (!newBadges.isEmpty()) {
            awardBadges(user.uid, newBadges, callback);
        } else {
            callback.onSuccess();
        }
    }
    
    private boolean hasBadge(Models.UserProfile user, String badgeId) {
        return user.badges != null && user.badges.contains(badgeId);
    }
    
    private void checkCourseBadges(Models.UserProfile user, List<String> newBadges) {
        // This would check course completion data from Firestore
        // For now, we'll implement basic badge logic
        FirebaseRefs.enrollments().whereEqualTo("userId", user.uid)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completedCourses = 0;
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Enrollment enrollment = doc.toObject(Models.Enrollment.class);
                        if (enrollment != null && enrollment.progress >= 100) {
                            completedCourses++;
                        }
                    }
                    
                    if (completedCourses >= 1 && !hasBadge(user, "first_course")) {
                        newBadges.add("first_course");
                    }
                    if (completedCourses >= 5 && !hasBadge(user, "course_explorer")) {
                        newBadges.add("course_explorer");
                    }
                    if (completedCourses >= 10 && !hasBadge(user, "course_master")) {
                        newBadges.add("course_master");
                    }
                });
    }
    
    private void awardBadges(String userId, List<String> badgeIds, GamificationCallback callback) {
        DocumentReference userRef = FirebaseRefs.users().document(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("badges", FieldValue.arrayUnion(badgeIds.toArray()));
        
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Awarded badges to user " + userId + ": " + badgeIds);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error awarding badges", e);
                    callback.onError("Failed to award badges: " + e.getMessage());
                });
    }
    
    // Update leaderboard entry for user
    private void updateLeaderboard(String userId) {
        FirebaseRefs.users().document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            calculateLeaderboardStats(user, stats -> {
                                Models.LeaderboardEntry entry = new Models.LeaderboardEntry();
                                entry.userId = user.uid;
                                entry.userName = user.name;
                                entry.userPhotoUrl = user.photoUrl;
                                entry.points = user.points;
                                entry.coursesCompleted = stats.coursesCompleted;
                                entry.eventsAttended = stats.eventsAttended;
                                entry.lecturesWatched = stats.lecturesWatched;
                                
                                FirebaseRefs.leaderboard().document(userId).set(entry.toMap());
                            });
                        }
                    }
                });
    }
    
    private static class LeaderboardStats {
        int coursesCompleted;
        int eventsAttended;
        int lecturesWatched;
    }
    
    private void calculateLeaderboardStats(Models.UserProfile user, java.util.function.Consumer<LeaderboardStats> callback) {
        LeaderboardStats stats = new LeaderboardStats();
        
        // Count completed courses
        FirebaseRefs.enrollments().whereEqualTo("userId", user.uid)
                .whereEqualTo("progress", 100)
                .get()
                .addOnSuccessListener(enrollmentSnapshot -> {
                    stats.coursesCompleted = enrollmentSnapshot.size();
                    
                    // Count attended events
                    FirebaseRefs.events().whereArrayContains("attendees", user.uid)
                            .get()
                            .addOnSuccessListener(eventSnapshot -> {
                                stats.eventsAttended = eventSnapshot.size();
                                
                                // Count watched lectures
                                FirebaseRefs.progress().whereEqualTo("userId", user.uid)
                                        .whereEqualTo("completed", true)
                                        .get()
                                        .addOnSuccessListener(progressSnapshot -> {
                                            stats.lecturesWatched = progressSnapshot.size();
                                            callback.accept(stats);
                                        });
                            });
                });
    }
    
    // Get leaderboard entries
    public void getLeaderboard(LeaderboardCallback callback) {
        FirebaseRefs.leaderboard().orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.LeaderboardEntry> entries = new ArrayList<>();
                    int rank = 1;
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.LeaderboardEntry entry = doc.toObject(Models.LeaderboardEntry.class);
                        if (entry != null) {
                            entry.rank = rank++;
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting leaderboard", e);
                    callback.onError("Failed to get leaderboard: " + e.getMessage());
                });
    }
    
    // Get user's badges
    public void getUserBadges(String userId, BadgeCallback callback) {
        FirebaseRefs.users().document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null && user.badges != null && !user.badges.isEmpty()) {
                            FirebaseRefs.badges().whereIn("id", user.badges).get()
                                    .addOnSuccessListener(badgeSnapshot -> {
                                        List<Models.Badge> badges = new ArrayList<>();
                                        for (var doc : badgeSnapshot.getDocuments()) {
                                            Models.Badge badge = doc.toObject(Models.Badge.class);
                                            if (badge != null) {
                                                badges.add(badge);
                                            }
                                        }
                                        callback.onSuccess(badges);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error getting badges", e);
                                        callback.onError("Failed to get badges: " + e.getMessage());
                                    });
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user badges", e);
                    callback.onError("Failed to get user badges: " + e.getMessage());
                });
    }
    
    // Initialize default badges in the system
    public void initializeDefaultBadges() {
        List<Models.Badge> defaultBadges = new ArrayList<>();
        
        // Points badges
        Models.Badge first100 = new Models.Badge();
        first100.id = "first_100";
        first100.name = "First Steps";
        first100.description = "Earned your first 100 points";
        first100.category = "points";
        first100.pointsReward = 10;
        defaultBadges.add(first100);
        
        Models.Badge pointCollector = new Models.Badge();
        pointCollector.id = "point_collector";
        pointCollector.name = "Point Collector";
        pointCollector.description = "Earned 500 points";
        pointCollector.category = "points";
        pointCollector.pointsReward = 25;
        defaultBadges.add(pointCollector);
        
        Models.Badge pointMaster = new Models.Badge();
        pointMaster.id = "point_master";
        pointMaster.name = "Point Master";
        pointMaster.description = "Earned 1000 points";
        pointMaster.category = "points";
        pointMaster.pointsReward = 50;
        defaultBadges.add(pointMaster);
        
        // Course badges
        Models.Badge firstCourse = new Models.Badge();
        firstCourse.id = "first_course";
        firstCourse.name = "First Course";
        firstCourse.description = "Completed your first course";
        firstCourse.category = "courses";
        firstCourse.pointsReward = 20;
        defaultBadges.add(firstCourse);
        
        Models.Badge courseExplorer = new Models.Badge();
        courseExplorer.id = "course_explorer";
        courseExplorer.name = "Course Explorer";
        courseExplorer.description = "Completed 5 courses";
        courseExplorer.category = "courses";
        courseExplorer.pointsReward = 50;
        defaultBadges.add(courseExplorer);
        
        Models.Badge courseMaster = new Models.Badge();
        courseMaster.id = "course_master";
        courseMaster.name = "Course Master";
        courseMaster.description = "Completed 10 courses";
        courseMaster.category = "courses";
        courseMaster.pointsReward = 100;
        defaultBadges.add(courseMaster);
        
        // Add badges to Firestore
        for (Models.Badge badge : defaultBadges) {
            FirebaseRefs.badges().document(badge.id).set(badge.toMap());
        }
    }
} 