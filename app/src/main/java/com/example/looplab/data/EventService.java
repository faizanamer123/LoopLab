package com.example.looplab.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventService {
    private static final String TAG = "EventService";
    
    public interface EventCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface EventListCallback {
        void onSuccess(List<Models.EventItem> events);
        void onError(String error);
    }
    
    public interface RegistrationCallback {
        void onSuccess(boolean registered);
        void onError(String error);
    }
    
    // Create a new event (for admins)
    public void createEvent(Models.EventItem event, EventCallback callback) {
        event.id = FirebaseRefs.events().document().getId();
        event.createdAt = System.currentTimeMillis();
        event.currentAttendees = 0;
        event.attendees = new ArrayList<>();
        
        FirebaseRefs.events().document(event.id).set(event.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event created: " + event.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating event", e);
                    callback.onError("Failed to create event: " + e.getMessage());
                });
    }
    
    // Update event
    public void updateEvent(Models.EventItem event, EventCallback callback) {
        FirebaseRefs.events().document(event.id).update(event.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event updated: " + event.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating event", e);
                    callback.onError("Failed to update event: " + e.getMessage());
                });
    }
    
    // Delete event
    public void deleteEvent(String eventId, EventCallback callback) {
        FirebaseRefs.events().document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    callback.onError("Failed to delete event: " + e.getMessage());
                });
    }
    
    // Get all upcoming events
    public void getUpcomingEvents(EventListCallback callback) {
        long currentTime = System.currentTimeMillis();
        
        FirebaseRefs.events().whereGreaterThan("startTime", currentTime)
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.EventItem> events = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.EventItem event = doc.toObject(Models.EventItem.class);
                        if (event != null) {
                            event.id = doc.getId();
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting upcoming events", e);
                    callback.onError("Failed to get upcoming events: " + e.getMessage());
                });
    }
    
    // Get past events
    public void getPastEvents(EventListCallback callback) {
        long currentTime = System.currentTimeMillis();
        
        FirebaseRefs.events().whereLessThan("endTime", currentTime)
                .orderBy("endTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.EventItem> events = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.EventItem event = doc.toObject(Models.EventItem.class);
                        if (event != null) {
                            event.id = doc.getId();
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting past events", e);
                    callback.onError("Failed to get past events: " + e.getMessage());
                });
    }
    
    // Register for event
    public void registerForEvent(String userId, String eventId, RegistrationCallback callback) {
        FirebaseRefs.events().document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.EventItem event = documentSnapshot.toObject(Models.EventItem.class);
                        if (event != null) {
                            // Check if user is already registered
                            if (event.attendees != null && event.attendees.contains(userId)) {
                                callback.onSuccess(true);
                                return;
                            }
                            
                            // Check if event is full
                            if (event.maxAttendees > 0 && event.currentAttendees >= event.maxAttendees) {
                                callback.onError("Event is full");
                                return;
                            }
                            
                            // Register user
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("attendees", FieldValue.arrayUnion(userId));
                            updates.put("currentAttendees", FieldValue.increment(1));
                            
                            FirebaseRefs.events().document(eventId).update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User registered for event: " + eventId);
                                        
                                        // Award points for event registration
                                        GamificationService gamificationService = new GamificationService();
                                        gamificationService.awardPoints(userId, 5, "Event registration", 
                                                new GamificationService.GamificationCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        callback.onSuccess(true);
                                                    }
                                                    
                                                    @Override
                                                    public void onError(String error) {
                                                        callback.onSuccess(true); // Registration succeeded even if points failed
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error registering for event", e);
                                        callback.onError("Failed to register: " + e.getMessage());
                                    });
                        } else {
                            callback.onError("Event not found");
                        }
                    } else {
                        callback.onError("Event not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting event", e);
                    callback.onError("Failed to get event: " + e.getMessage());
                });
    }
    
    // Add event to Google Calendar
    public void addToGoogleCalendar(Context context, Models.EventItem event) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            String startTime = sdf.format(new Date(event.startTime));
            String endTime = sdf.format(new Date(event.endTime));
            
            String calendarUrl = String.format(
                "https://calendar.google.com/calendar/render?" +
                "action=TEMPLATE&" +
                "text=%s&" +
                "dates=%s/%s&" +
                "details=%s&" +
                "location=%s",
                Uri.encode(event.title),
                startTime.replace(":", "").replace("-", ""),
                endTime.replace(":", "").replace("-", ""),
                Uri.encode(event.description),
                Uri.encode(event.location)
            );
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl));
            context.startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding to calendar", e);
        }
    }
} 