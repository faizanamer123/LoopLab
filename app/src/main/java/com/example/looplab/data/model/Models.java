package com.example.looplab.data.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Models {
    public static class UserProfile {
        public String uid;
        public String name;
        public String email;
        public String role; // admin | teacher | student
        public String photoUrl;
        public String bio;
        public List<String> badges;
        public int points;
        public boolean verified;
        public long createdAt;
        public long lastActive;
        public boolean isFirstTime;
        public boolean isActive;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("uid", uid);
            m.put("name", name);
            m.put("email", email);
            m.put("role", role);
            m.put("photoUrl", photoUrl);
            m.put("bio", bio);
            m.put("badges", badges);
            m.put("points", points);
            m.put("verified", verified);
            m.put("createdAt", createdAt);
            m.put("lastActive", lastActive);
            m.put("isFirstTime", isFirstTime);
            m.put("isActive", isActive);
            return m;
        }
    }

    public static class Course {
        public String id;
        public String title;
        public String description;
        public String instructorId;
        public String instructorName;
        public String thumbnailUrl;
        public int lectureCount;
        public int enrolledCount;
        public String category;
        public String difficulty;
        public long createdAt;
        public boolean isPublished;
        public List<String> tags;
        public double rating;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("title", title);
            m.put("description", description);
            m.put("instructorId", instructorId);
            m.put("instructorName", instructorName);
            m.put("thumbnailUrl", thumbnailUrl);
            m.put("lectureCount", lectureCount);
            m.put("enrolledCount", enrolledCount);
            m.put("category", category);
            m.put("difficulty", difficulty);
            m.put("createdAt", createdAt);
            m.put("isPublished", isPublished);
            m.put("tags", tags);
            m.put("rating", rating);
            return m;
        }
    }

    public static class Lecture {
        public String id;
        public String courseId;
        public String title;
        public String description;
        public String videoUrl;
        public String thumbnailUrl;
        public int duration; // in seconds
        public int order;
        public long createdAt;
        public boolean isPublished;
        public boolean completed; // Added completed property

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("courseId", courseId);
            m.put("title", title);
            m.put("description", description);
            m.put("videoUrl", videoUrl);
            m.put("thumbnailUrl", thumbnailUrl);
            m.put("duration", duration);
            m.put("order", order);
            m.put("createdAt", createdAt);
            m.put("isPublished", isPublished);
            m.put("completed", completed); // Added completed to map
            return m;
        }
    }

    public static class Enrollment {
        public String id;
        public String userId;
        public String courseId;
        public long enrolledAt;
        public boolean isActive;
        public int progress; // percentage
        public long lastAccessed;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("userId", userId);
            m.put("courseId", courseId);
            m.put("enrolledAt", enrolledAt);
            m.put("isActive", isActive);
            m.put("progress", progress);
            m.put("lastAccessed", lastAccessed);
            return m;
        }
    }

    public static class Progress {
        public String id;
        public String userId;
        public String courseId;
        public String lectureId;
        public boolean completed;
        public int watchTime; // in seconds
        public long lastWatched;
        public long completedAt;
        public List<String> completedLectures; // List of completed lecture IDs

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("userId", userId);
            m.put("courseId", courseId);
            m.put("lectureId", lectureId);
            m.put("completed", completed);
            m.put("watchTime", watchTime);
            m.put("lastWatched", lastWatched);
            m.put("completedAt", completedAt);
            m.put("completedLectures", completedLectures);
            return m;
        }
    }

    public static class Badge {
        public String id;
        public String name;
        public String description;
        public String iconUrl;
        public String category;
        public int pointsReward;
        public String criteria; // JSON string describing how to earn

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("description", description);
            m.put("iconUrl", iconUrl);
            m.put("category", category);
            m.put("pointsReward", pointsReward);
            m.put("criteria", criteria);
            return m;
        }
    }

    public static class LeaderboardEntry {
        public String userId;
        public String userName;
        public String userPhotoUrl;
        public int points;
        public int rank;
        public int coursesCompleted;
        public int eventsAttended;
        public int lecturesWatched;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            m.put("userName", userName);
            m.put("userPhotoUrl", userPhotoUrl);
            m.put("points", points);
            m.put("rank", rank);
            m.put("coursesCompleted", coursesCompleted);
            m.put("eventsAttended", eventsAttended);
            m.put("lecturesWatched", lecturesWatched);
            return m;
        }
    }

    public static class Announcement {
        public String id;
        public String title;
        public String body;
        public long createdAt;
        public String createdBy;
        public String createdByName;
        public List<String> targetRoles; // null for all, or specific roles
        public boolean isImportant;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("title", title);
            m.put("body", body);
            m.put("createdAt", createdAt);
            m.put("createdBy", createdBy);
            m.put("createdByName", createdByName);
            m.put("targetRoles", targetRoles);
            m.put("isImportant", isImportant);
            return m;
        }
    }

    public static class EventItem {
        public String id;
        public String title;
        public String description;
        public long startTime;
        public long endTime;
        public String location;
        public String bannerUrl;
        public String organizerId;
        public String organizerName;
        public int maxAttendees;
        public int currentAttendees;
        public List<String> attendees;
        public String category;
        public boolean isOnline;
        public String meetingUrl;
        public boolean registered;
        public long createdAt;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("title", title);
            m.put("description", description);
            m.put("startTime", startTime);
            m.put("endTime", endTime);
            m.put("location", location);
            m.put("bannerUrl", bannerUrl);
            m.put("organizerId", organizerId);
            m.put("organizerName", organizerName);
            m.put("maxAttendees", maxAttendees);
            m.put("currentAttendees", currentAttendees);
            m.put("attendees", attendees);
            m.put("category", category);
            m.put("isOnline", isOnline);
            m.put("meetingUrl", meetingUrl);
            m.put("registered", registered);
            m.put("createdAt", createdAt);
            return m;
        }
    }

    public static class Chat {
        public String id;
        public String type; // "1:1" or "group"
        public String name; // for group chats
        public List<String> participants;
        public String lastMessage;
        public long lastMessageTime;
        public String lastMessageSender;
        public long createdAt;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("type", type);
            m.put("name", name);
            m.put("participants", participants);
            m.put("lastMessage", lastMessage);
            m.put("lastMessageTime", lastMessageTime);
            m.put("lastMessageSender", lastMessageSender);
            m.put("createdAt", createdAt);
            return m;
        }
    }

    public static class Message {
        public String id;
        public String chatId;
        public String senderId;
        public String senderName;
        public String content;
        public String type; // "text", "image", "file"
        public String mediaUrl;
        public long timestamp;
        public boolean isRead;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("chatId", chatId);
            m.put("senderId", senderId);
            m.put("senderName", senderName);
            m.put("content", content);
            m.put("type", type);
            m.put("mediaUrl", mediaUrl);
            m.put("timestamp", timestamp);
            m.put("isRead", isRead);
            return m;
        }
    }

    public static class TeamMember {
        public String id;
        public String name;
        public String role;
        public String bio;
        public String email;
        public String photoUrl;
        public boolean isActive;
        public long createdAt;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("role", role);
            m.put("bio", bio);
            m.put("email", email);
            m.put("photoUrl", photoUrl);
            m.put("isActive", isActive);
            m.put("createdAt", createdAt);
            return m;
        }
    }

    public static class Feedback {
        public String id;
        public String userId;
        public String userName;
        public String subject;
        public String message;
        public String type; // general, bug, feature, complaint
        public String status; // pending, in_progress, resolved, closed
        public long createdAt;
        public String adminResponse;
        public long respondedAt;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("userId", userId);
            m.put("userName", userName);
            m.put("subject", subject);
            m.put("message", message);
            m.put("type", type);
            m.put("status", status);
            m.put("createdAt", createdAt);
            m.put("adminResponse", adminResponse);
            m.put("respondedAt", respondedAt);
            return m;
        }
    }

    public static class Analytics {
        public String id;
        public String type; // "daily", "weekly", "monthly"
        public long date;
        public int totalUsers;
        public int activeUsers;
        public int coursesCreated;
        public int enrollments;
        public int eventsCreated;
        public int eventsAttended;
        public int messagesSent;
        public Map<String, Object> data;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("type", type);
            m.put("date", date);
            m.put("totalUsers", totalUsers);
            m.put("activeUsers", activeUsers);
            m.put("coursesCreated", coursesCreated);
            m.put("enrollments", enrollments);
            m.put("eventsCreated", eventsCreated);
            m.put("eventsAttended", eventsAttended);
            m.put("messagesSent", messagesSent);
            m.put("data", data);
            return m;
        }
    }

    public static class Conversation {
        public String id;
        public String name;
        public String type; // "1:1", "group", "ai"
        public List<String> participants;
        public String lastMessage;
        public long lastMessageTime;
        public String lastMessageSender;
        public long createdAt;
        public String photoUrl;
        public boolean isActive;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("type", type);
            m.put("participants", participants);
            m.put("lastMessage", lastMessage);
            m.put("lastMessageTime", lastMessageTime);
            m.put("lastMessageSender", lastMessageSender);
            m.put("createdAt", createdAt);
            m.put("photoUrl", photoUrl);
            m.put("isActive", isActive);
            return m;
        }
    }
}


