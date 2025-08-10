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

public class CourseService {
    private static final String TAG = "CourseService";
    
    public interface CourseCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface CourseListCallback {
        void onSuccess(List<Models.Course> courses);
        void onError(String error);
    }
    
    public interface LectureCallback {
        void onSuccess(List<Models.Lecture> lectures);
        void onError(String error);
    }
    
    public interface EnrollmentCallback {
        void onSuccess(Models.Enrollment enrollment);
        void onError(String error);
    }
    
    public interface ProgressCallback {
        void onSuccess(Models.Progress progress);
        void onError(String error);
    }
    
    public interface SingleCourseCallback {
        void onSuccess(Models.Course course);
        void onError(String error);
    }
    
    // Create a new course (for teachers)
    public void createCourse(Models.Course course, CourseCallback callback) {
        course.id = FirebaseRefs.courses().document().getId();
        course.createdAt = System.currentTimeMillis();
        course.isPublished = false;
        
        FirebaseRefs.courses().document(course.id).set(course.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Course created: " + course.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating course", e);
                    callback.onError("Failed to create course: " + e.getMessage());
                });
    }
    
    // Update course
    public void updateCourse(Models.Course course, CourseCallback callback) {
        FirebaseRefs.courses().document(course.id).update(course.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Course updated: " + course.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating course", e);
                    callback.onError("Failed to update course: " + e.getMessage());
                });
    }
    
    // Delete course
    public void deleteCourse(String courseId, CourseCallback callback) {
        FirebaseRefs.courses().document(courseId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Course deleted: " + courseId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting course", e);
                    callback.onError("Failed to delete course: " + e.getMessage());
                });
    }
    
    // Get all published courses
    public void getPublishedCourses(CourseListCallback callback) {
        FirebaseRefs.courses().whereEqualTo("isPublished", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Course> courses = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            course.id = doc.getId();
                            courses.add(course);
                        }
                    }
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting courses", e);
                    callback.onError("Failed to get courses: " + e.getMessage());
                });
    }
    
    // Get all courses (for admin management)
    public void getAllCourses(CourseListCallback callback) {
        FirebaseRefs.courses().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Course> courses = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            course.id = doc.getId();
                            courses.add(course);
                        }
                    }
                    // Sort courses by createdAt in descending order (newest first) in memory
                    courses.sort((c1, c2) -> Long.compare(c2.createdAt, c1.createdAt));
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all courses", e);
                    callback.onError("Failed to get all courses: " + e.getMessage());
                });
    }

    // Get a single course by ID
    public void getCourse(String courseId, SingleCourseCallback callback) {
        FirebaseRefs.courses().document(courseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.Course course = documentSnapshot.toObject(Models.Course.class);
                        if (course != null) {
                            course.id = documentSnapshot.getId();
                            callback.onSuccess(course);
                        } else {
                            callback.onError("Failed to parse course data");
                        }
                    } else {
                        callback.onError("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting course", e);
                    callback.onError("Failed to get course: " + e.getMessage());
                });
    }
    
    // Get courses by instructor
    public void getCoursesByInstructor(String instructorId, CourseListCallback callback) {
        FirebaseRefs.courses().whereEqualTo("instructorId", instructorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Course> courses = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Course course = doc.toObject(Models.Course.class);
                        if (course != null) {
                            course.id = doc.getId();
                            courses.add(course);
                        }
                    }
                    // Sort courses by createdAt in descending order (newest first) in memory
                    courses.sort((c1, c2) -> Long.compare(c2.createdAt, c1.createdAt));
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting instructor courses", e);
                    callback.onError("Failed to get instructor courses: " + e.getMessage());
                });
    }
    
    // Add lecture to course
    public void addLecture(Models.Lecture lecture, CourseCallback callback) {
        lecture.id = FirebaseRefs.lectures().document().getId();
        lecture.createdAt = System.currentTimeMillis();
        // Keep the isPublished value from the lecture object
        
        // Log the lecture data before saving
        Log.d(TAG, "Adding lecture - ID: " + lecture.id + ", Title: " + lecture.title + 
              ", VideoURL: " + lecture.videoUrl + ", isPublished: " + lecture.isPublished);
        
        FirebaseRefs.lectures().document(lecture.id).set(lecture.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Lecture added successfully: " + lecture.id + " with video URL: " + lecture.videoUrl);
                    updateCourseLectureCount(lecture.courseId, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding lecture", e);
                    callback.onError("Failed to add lecture: " + e.getMessage());
                });
    }
    
    // Update lecture
    public void updateLecture(Models.Lecture lecture, CourseCallback callback) {
        FirebaseRefs.lectures().document(lecture.id).update(lecture.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Lecture updated: " + lecture.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating lecture", e);
                    callback.onError("Failed to update lecture: " + e.getMessage());
                });
    }
    
    // Get lectures for a course
    public void getCourseLectures(String courseId, LectureCallback callback) {
        Log.d(TAG, "Getting lectures for course: " + courseId);
        FirebaseRefs.lectures().whereEqualTo("courseId", courseId)
                .whereEqualTo("isPublished", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Lecture> lectures = new ArrayList<>();
                    Log.d(TAG, "Found " + querySnapshot.size() + " published lectures for course: " + courseId);
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Lecture lecture = doc.toObject(Models.Lecture.class);
                        if (lecture != null) {
                            lecture.id = doc.getId();
                            lectures.add(lecture);
                            Log.d(TAG, "Lecture loaded - ID: " + lecture.id + ", Title: " + lecture.title + 
                                  ", VideoURL: " + lecture.videoUrl + ", isPublished: " + lecture.isPublished);
                        }
                    }
                    // Sort lectures by order in ascending order in memory
                    lectures.sort((l1, l2) -> Integer.compare(l1.order, l2.order));
                    Log.d(TAG, "Returning " + lectures.size() + " sorted lectures");
                    callback.onSuccess(lectures);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting lectures", e);
                    callback.onError("Failed to get lectures: " + e.getMessage());
                });
    }
    
    // Enroll student in course
    public void enrollInCourse(String userId, String courseId, EnrollmentCallback callback) {
        // Check if already enrolled
        FirebaseRefs.enrollments().whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Create new enrollment
                        Models.Enrollment enrollment = new Models.Enrollment();
                        enrollment.id = FirebaseRefs.enrollments().document().getId();
                        enrollment.userId = userId;
                        enrollment.courseId = courseId;
                        enrollment.enrolledAt = System.currentTimeMillis();
                        enrollment.isActive = true;
                        enrollment.progress = 0;
                        enrollment.lastAccessed = System.currentTimeMillis();
                        
                        FirebaseRefs.enrollments().document(enrollment.id).set(enrollment.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Enrolled in course: " + courseId);
                                    updateCourseEnrollmentCount(courseId, true);
                                    callback.onSuccess(enrollment);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error enrolling in course", e);
                                    callback.onError("Failed to enroll: " + e.getMessage());
                                });
                    } else {
                        // Already enrolled
                        Models.Enrollment enrollment = querySnapshot.getDocuments().get(0).toObject(Models.Enrollment.class);
                        if (enrollment != null) {
                            enrollment.id = querySnapshot.getDocuments().get(0).getId();
                            callback.onSuccess(enrollment);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking enrollment", e);
                    callback.onError("Failed to check enrollment: " + e.getMessage());
                });
    }
    
    // Get user enrollments
    public void getUserEnrollments(String userId, CourseListCallback callback) {
        FirebaseRefs.enrollments().whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(enrollmentSnapshot -> {
                    List<String> courseIds = new ArrayList<>();
                    for (var doc : enrollmentSnapshot.getDocuments()) {
                        Models.Enrollment enrollment = doc.toObject(Models.Enrollment.class);
                        if (enrollment != null) {
                            courseIds.add(enrollment.courseId);
                        }
                    }
                    
                    if (courseIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    // Get course details
                    FirebaseRefs.courses().whereIn("id", courseIds).get()
                            .addOnSuccessListener(courseSnapshot -> {
                                List<Models.Course> courses = new ArrayList<>();
                                for (var doc : courseSnapshot.getDocuments()) {
                                    Models.Course course = doc.toObject(Models.Course.class);
                                    if (course != null) {
                                        course.id = doc.getId();
                                        courses.add(course);
                                    }
                                }
                                callback.onSuccess(courses);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting enrolled courses", e);
                                callback.onError("Failed to get enrolled courses: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting enrollments", e);
                    callback.onError("Failed to get enrollments: " + e.getMessage());
                });
    }
    
    // Update lecture progress
    public void updateLectureProgress(String userId, String courseId, String lectureId, 
                                    int watchTime, boolean completed, ProgressCallback callback) {
        String progressId = userId + "_" + courseId + "_" + lectureId;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("userId", userId);
        updates.put("courseId", courseId);
        updates.put("lectureId", lectureId);
        updates.put("watchTime", watchTime);
        updates.put("lastWatched", System.currentTimeMillis());
        
        if (completed) {
            updates.put("completed", true);
            updates.put("completedAt", System.currentTimeMillis());
        }
        
        FirebaseRefs.progress().document(progressId).set(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Progress updated for lecture: " + lectureId);
                    
                    // Update course progress
                    updateCourseProgress(userId, courseId);
                    
                    // Award points for completion
                    if (completed) {
                        GamificationService gamificationService = new GamificationService();
                        gamificationService.awardPoints(userId, 10, "Lecture completed", 
                                new GamificationService.GamificationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Models.Progress progress = new Models.Progress();
                                        progress.id = progressId;
                                        progress.userId = userId;
                                        progress.courseId = courseId;
                                        progress.lectureId = lectureId;
                                        progress.watchTime = watchTime;
                                        progress.completed = completed;
                                        progress.lastWatched = System.currentTimeMillis();
                                        if (completed) {
                                            progress.completedAt = System.currentTimeMillis();
                                        }
                                        callback.onSuccess(progress);
                                    }
                                    
                                    @Override
                                    public void onError(String error) {
                                        callback.onError(error);
                                    }
                                });
                    } else {
                        Models.Progress progress = new Models.Progress();
                        progress.id = progressId;
                        progress.userId = userId;
                        progress.courseId = courseId;
                        progress.lectureId = lectureId;
                        progress.watchTime = watchTime;
                        progress.completed = completed;
                        progress.lastWatched = System.currentTimeMillis();
                        callback.onSuccess(progress);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating progress", e);
                    callback.onError("Failed to update progress: " + e.getMessage());
                });
    }
    
    // Get course progress for user
    public void getCourseProgress(String userId, String courseId, ProgressCallback callback) {
        FirebaseRefs.progress().whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completedLecturesCount = 0;
                    int totalWatchTimeSum = 0;
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Progress progress = doc.toObject(Models.Progress.class);
                        if (progress != null && progress.completed) {
                            completedLecturesCount++;
                        }
                        if (progress != null) {
                            totalWatchTimeSum += progress.watchTime;
                        }
                    }
                    
                    // Make variables effectively final
                    final int completedLectures = completedLecturesCount;
                    final int totalWatchTime = totalWatchTimeSum;
                    
                    // Calculate overall progress
                    FirebaseRefs.lectures().whereEqualTo("courseId", courseId)
                            .whereEqualTo("isPublished", true)
                            .get()
                            .addOnSuccessListener(lectureSnapshot -> {
                                int totalLectures = lectureSnapshot.size();
                                int progressPercentage = totalLectures > 0 ? (completedLectures * 100) / totalLectures : 0;
                                
                                // Update enrollment progress
                                FirebaseRefs.enrollments().whereEqualTo("userId", userId)
                                        .whereEqualTo("courseId", courseId)
                                        .get()
                                        .addOnSuccessListener(enrollmentSnapshot -> {
                                            if (!enrollmentSnapshot.isEmpty()) {
                                                String enrollmentId = enrollmentSnapshot.getDocuments().get(0).getId();
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("progress", progressPercentage);
                                                updates.put("lastAccessed", System.currentTimeMillis());
                                                
                                                FirebaseRefs.enrollments().document(enrollmentId).update(updates);
                                            }
                                        });
                                
                                Models.Progress overallProgress = new Models.Progress();
                                overallProgress.userId = userId;
                                overallProgress.courseId = courseId;
                                overallProgress.completed = progressPercentage >= 100;
                                overallProgress.watchTime = totalWatchTime;
                                callback.onSuccess(overallProgress);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting course progress", e);
                    callback.onError("Failed to get course progress: " + e.getMessage());
                });
    }
    
    // Get user progress for a specific course (simplified version)
    public void getUserProgress(String userId, String courseId, ProgressCallback callback) {
        Log.d(TAG, "Getting user progress for userId: " + userId + ", courseId: " + courseId);
        
        FirebaseRefs.progress().whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " progress documents");
                    
                    Models.Progress userProgress = new Models.Progress();
                    userProgress.userId = userId;
                    userProgress.courseId = courseId;
                    userProgress.completedLectures = new ArrayList<>();
                    userProgress.watchTime = 0;
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Raw document data: " + doc.getData());
                        Models.Progress progress = doc.toObject(Models.Progress.class);
                        if (progress != null) {
                            Log.d(TAG, "Progress doc - lectureId: " + progress.lectureId + 
                                  ", completed: " + progress.completed + ", watchTime: " + progress.watchTime);
                            if (progress.completed) {
                                userProgress.completedLectures.add(progress.lectureId);
                                Log.d(TAG, "Added completed lecture: " + progress.lectureId);
                            }
                            userProgress.watchTime += progress.watchTime;
                        } else {
                            Log.e(TAG, "Failed to convert document to Progress object");
                        }
                    }
                    
                    Log.d(TAG, "Final user progress - completedLectures: " + userProgress.completedLectures.size() + 
                          ", total watchTime: " + userProgress.watchTime);
                    
                    callback.onSuccess(userProgress);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user progress", e);
                    callback.onError("Failed to get user progress: " + e.getMessage());
                });
    }
    
    private void updateCourseLectureCount(String courseId, CourseCallback callback) {
        FirebaseRefs.lectures().whereEqualTo("courseId", courseId)
                .whereEqualTo("isPublished", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lectureCount", querySnapshot.size());
                    
                    FirebaseRefs.courses().document(courseId).update(updates)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError("Failed to update lecture count"));
                });
    }
    
    private void updateCourseEnrollmentCount(String courseId, boolean increment) {
        Map<String, Object> updates = new HashMap<>();
        if (increment) {
            updates.put("enrolledCount", FieldValue.increment(1));
        } else {
            updates.put("enrolledCount", FieldValue.increment(-1));
        }
        
        FirebaseRefs.courses().document(courseId).update(updates);
    }
    
    private void updateCourseProgress(String userId, String courseId) {
        // This method updates the overall course progress
        // Implementation is in getCourseProgress method
    }
} 