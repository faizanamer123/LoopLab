package com.example.looplab.data;

import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackService {
    private static final String TAG = "FeedbackService";
    
    public interface FeedbackCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface FeedbackListCallback {
        void onSuccess(List<Models.Feedback> feedbacks);
        void onError(String error);
    }
    
    public interface AnalyticsCallback {
        void onSuccess(Models.Analytics analytics);
        void onError(String error);
    }
    
    // Submit feedback
    public void submitFeedback(Models.Feedback feedback, FeedbackCallback callback) {
        feedback.id = FirebaseRefs.feedback().document().getId();
        feedback.createdAt = System.currentTimeMillis();
        feedback.status = "open";
        
        FirebaseRefs.feedback().document(feedback.id).set(feedback.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback submitted: " + feedback.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting feedback", e);
                    callback.onError("Failed to submit feedback: " + e.getMessage());
                });
    }
    
    // Update feedback status
    public void updateFeedbackStatus(String feedbackId, String status, String assignedTo, FeedbackCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (assignedTo != null) {
            updates.put("assignedTo", assignedTo);
        }
        
        FirebaseRefs.feedback().document(feedbackId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback status updated: " + feedbackId + " to " + status);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating feedback status", e);
                    callback.onError("Failed to update feedback status: " + e.getMessage());
                });
    }
    
    // Get all feedback (for admins)
    public void getAllFeedback(FeedbackListCallback callback) {
        FirebaseRefs.feedback().orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Feedback> feedbacks = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Feedback feedback = doc.toObject(Models.Feedback.class);
                        if (feedback != null) {
                            feedback.id = doc.getId();
                            feedbacks.add(feedback);
                        }
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting feedback", e);
                    callback.onError("Failed to get feedback: " + e.getMessage());
                });
    }
    
    // Get feedback by user
    public void getUserFeedback(String userId, FeedbackListCallback callback) {
        FirebaseRefs.feedback().whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Feedback> feedbacks = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Feedback feedback = doc.toObject(Models.Feedback.class);
                        if (feedback != null) {
                            feedback.id = doc.getId();
                            feedbacks.add(feedback);
                        }
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user feedback", e);
                    callback.onError("Failed to get user feedback: " + e.getMessage());
                });
    }
    
    // Get feedback by status
    public void getFeedbackByStatus(String status, FeedbackListCallback callback) {
        FirebaseRefs.feedback().whereEqualTo("status", status)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Feedback> feedbacks = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Feedback feedback = doc.toObject(Models.Feedback.class);
                        if (feedback != null) {
                            feedback.id = doc.getId();
                            feedbacks.add(feedback);
                        }
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting feedback by status", e);
                    callback.onError("Failed to get feedback by status: " + e.getMessage());
                });
    }
    
    // Get feedback by type
    public void getFeedbackByType(String type, FeedbackListCallback callback) {
        FirebaseRefs.feedback().whereEqualTo("type", type)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Feedback> feedbacks = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Feedback feedback = doc.toObject(Models.Feedback.class);
                        if (feedback != null) {
                            feedback.id = doc.getId();
                            feedbacks.add(feedback);
                        }
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting feedback by type", e);
                    callback.onError("Failed to get feedback by type: " + e.getMessage());
                });
    }
    
    // Delete feedback
    public void deleteFeedback(String feedbackId, FeedbackCallback callback) {
        FirebaseRefs.feedback().document(feedbackId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback deleted: " + feedbackId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting feedback", e);
                    callback.onError("Failed to delete feedback: " + e.getMessage());
                });
    }
    
    // Get feedback statistics
    public void getFeedbackStats(FeedbackCallback callback) {
        FirebaseRefs.feedback().get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalFeedback = querySnapshot.size();
                    int openFeedback = 0;
                    int inProgressFeedback = 0;
                    int resolvedFeedback = 0;
                    int bugReports = 0;
                    int featureRequests = 0;
                    int generalFeedback = 0;
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Feedback feedback = doc.toObject(Models.Feedback.class);
                        if (feedback != null) {
                            switch (feedback.status) {
                                case "open":
                                    openFeedback++;
                                    break;
                                case "in_progress":
                                    inProgressFeedback++;
                                    break;
                                case "resolved":
                                    resolvedFeedback++;
                                    break;
                            }
                            
                            switch (feedback.type) {
                                case "bug":
                                    bugReports++;
                                    break;
                                case "feature":
                                    featureRequests++;
                                    break;
                                case "general":
                                    generalFeedback++;
                                    break;
                            }
                        }
                    }
                    
                    Log.d(TAG, String.format("Feedback stats - Total: %d, Open: %d, In Progress: %d, Resolved: %d",
                            totalFeedback, openFeedback, inProgressFeedback, resolvedFeedback));
                    Log.d(TAG, String.format("Feedback types - Bugs: %d, Features: %d, General: %d",
                            bugReports, featureRequests, generalFeedback));
                    
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting feedback stats", e);
                    callback.onError("Failed to get feedback stats: " + e.getMessage());
                });
    }
    
    // Create analytics entry
    public void createAnalyticsEntry(String type, long date, AnalyticsCallback callback) {
        Models.Analytics analytics = new Models.Analytics();
        analytics.id = FirebaseRefs.analytics().document().getId();
        analytics.type = type;
        analytics.date = date;
        analytics.data = new HashMap<>();
        
        // Get current statistics
        getCurrentStats(analytics, callback);
    }
    
    private void getCurrentStats(Models.Analytics analytics, AnalyticsCallback callback) {
        // Get user count
        FirebaseRefs.users().get()
                .addOnSuccessListener(userSnapshot -> {
                    analytics.totalUsers = userSnapshot.size();
                    
                    // Get active users (users active in last 7 days)
                    long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                    FirebaseRefs.users().whereGreaterThan("lastActive", weekAgo).get()
                            .addOnSuccessListener(activeSnapshot -> {
                                analytics.activeUsers = activeSnapshot.size();
                                
                                // Get course statistics
                                FirebaseRefs.courses().get()
                                        .addOnSuccessListener(courseSnapshot -> {
                                            analytics.coursesCreated = courseSnapshot.size();
                                            
                                            // Get enrollment count
                                            FirebaseRefs.enrollments().get()
                                                    .addOnSuccessListener(enrollmentSnapshot -> {
                                                        analytics.enrollments = enrollmentSnapshot.size();
                                                        
                                                        // Get event statistics
                                                        FirebaseRefs.events().get()
                                                                .addOnSuccessListener(eventSnapshot -> {
                                                                    analytics.eventsCreated = eventSnapshot.size();
                                                                    
                                                                    // Get message count
                                                                    FirebaseRefs.messages().get()
                                                                            .addOnSuccessListener(messageSnapshot -> {
                                                                                analytics.messagesSent = messageSnapshot.size();
                                                                                
                                                                                // Save analytics
                                                                                FirebaseRefs.analytics().document(analytics.id).set(analytics.toMap())
                                                                                        .addOnSuccessListener(aVoid -> {
                                                                                            Log.d(TAG, "Analytics entry created: " + analytics.id);
                                                                                            callback.onSuccess(analytics);
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e(TAG, "Error saving analytics", e);
                                                                                            callback.onError("Failed to save analytics: " + e.getMessage());
                                                                                        });
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e(TAG, "Error getting messages", e);
                                                                                callback.onError("Failed to get messages: " + e.getMessage());
                                                                            });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e(TAG, "Error getting events", e);
                                                                    callback.onError("Failed to get events: " + e.getMessage());
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error getting enrollments", e);
                                                        callback.onError("Failed to get enrollments: " + e.getMessage());
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error getting courses", e);
                                            callback.onError("Failed to get courses: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting active users", e);
                                callback.onError("Failed to get active users: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users", e);
                    callback.onError("Failed to get users: " + e.getMessage());
                });
    }
    
    // Get analytics by type
    public void getAnalyticsByType(String type, AnalyticsCallback callback) {
        FirebaseRefs.analytics().whereEqualTo("type", type)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Models.Analytics analytics = querySnapshot.getDocuments().get(0).toObject(Models.Analytics.class);
                        if (analytics != null) {
                            analytics.id = querySnapshot.getDocuments().get(0).getId();
                            callback.onSuccess(analytics);
                        } else {
                            callback.onError("Analytics data not found");
                        }
                    } else {
                        callback.onError("No analytics data found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting analytics", e);
                    callback.onError("Failed to get analytics: " + e.getMessage());
                });
    }
    
    // Get analytics summary
    public void getAnalyticsSummary(AnalyticsCallback callback) {
        FirebaseRefs.analytics().orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Models.Analytics analytics = querySnapshot.getDocuments().get(0).toObject(Models.Analytics.class);
                        if (analytics != null) {
                            analytics.id = querySnapshot.getDocuments().get(0).getId();
                            callback.onSuccess(analytics);
                        } else {
                            callback.onError("Analytics data not found");
                        }
                    } else {
                        callback.onError("No analytics data found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting analytics summary", e);
                    callback.onError("Failed to get analytics summary: " + e.getMessage());
                });
    }
} 