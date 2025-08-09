package com.example.looplab.data;

import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {
    private static final String TAG = "ChatService";
    
    public interface ChatCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface ChatListCallback {
        void onSuccess(List<Models.Chat> chats);
        void onError(String error);
    }
    
    public interface MessageCallback {
        void onSuccess(List<Models.Message> messages);
        void onError(String error);
    }
    
    public interface MessageListener {
        void onMessageReceived(Models.Message message);
        void onError(String error);
    }

    public interface ChatIdCallback {
        void onSuccess(String chatId);
        void onError(String error);
    }
    
    // Create a new 1:1 chat
    public void createOneToOneChat(String userId1, String userId2, ChatCallback callback) {
        // Check if chat already exists
        FirebaseRefs.chats().whereEqualTo("type", "1:1")
                .whereArrayContains("participants", userId1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Chat chat = doc.toObject(Models.Chat.class);
                        if (chat != null && chat.participants.contains(userId2)) {
                            // Chat already exists
                            callback.onSuccess();
                            return;
                        }
                    }
                    
                    // Create new chat
                    Models.Chat chat = new Models.Chat();
                    chat.id = FirebaseRefs.chats().document().getId();
                    chat.type = "1:1";
                    chat.participants = new ArrayList<>();
                    chat.participants.add(userId1);
                    chat.participants.add(userId2);
                    chat.createdAt = System.currentTimeMillis();
                    
                    FirebaseRefs.chats().document(chat.id).set(chat.toMap())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "1:1 chat created: " + chat.id);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating 1:1 chat", e);
                                callback.onError("Failed to create chat: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing chat", e);
                    callback.onError("Failed to check existing chat: " + e.getMessage());
                });
    }

    // Get existing 1:1 chat id for two users, or create one and return its id
    public void getOrCreateOneToOneChat(String userId1, String userId2, ChatIdCallback callback) {
        FirebaseRefs.chats().whereEqualTo("type", "1:1")
                .whereArrayContains("participants", userId1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Chat chat = doc.toObject(Models.Chat.class);
                        if (chat != null && chat.participants != null && chat.participants.contains(userId2)) {
                            callback.onSuccess(doc.getId());
                            return;
                        }
                    }

                    // Create new chat
                    Models.Chat chat = new Models.Chat();
                    chat.id = FirebaseRefs.chats().document().getId();
                    chat.type = "1:1";
                    chat.participants = new ArrayList<>();
                    chat.participants.add(userId1);
                    chat.participants.add(userId2);
                    chat.createdAt = System.currentTimeMillis();

                    FirebaseRefs.chats().document(chat.id).set(chat.toMap())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "1:1 chat created: " + chat.id);
                                callback.onSuccess(chat.id);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating 1:1 chat", e);
                                callback.onError("Failed to create chat: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing chat", e);
                    callback.onError("Failed to check existing chat: " + e.getMessage());
                });
    }
    
    // Create a new group chat
    public void createGroupChat(String name, List<String> participants, ChatCallback callback) {
        Models.Chat chat = new Models.Chat();
        chat.id = FirebaseRefs.chats().document().getId();
        chat.type = "group";
        chat.name = name;
        chat.participants = participants;
        chat.createdAt = System.currentTimeMillis();
        
        FirebaseRefs.chats().document(chat.id).set(chat.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Group chat created: " + chat.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating group chat", e);
                    callback.onError("Failed to create group chat: " + e.getMessage());
                });
    }
    
    // Get user's chats
    public void getUserChats(String userId, ChatListCallback callback) {
        FirebaseRefs.chats().whereArrayContains("participants", userId)
                .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Chat> chats = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Chat chat = doc.toObject(Models.Chat.class);
                        if (chat != null) {
                            chat.id = doc.getId();
                            chats.add(chat);
                        }
                    }
                    callback.onSuccess(chats);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user chats", e);
                    callback.onError("Failed to get chats: " + e.getMessage());
                });
    }
    
    // Send message
    public void sendMessage(String chatId, String senderId, String content, String type, ChatCallback callback) {
        Models.Message message = new Models.Message();
        message.id = FirebaseRefs.messages().document().getId();
        message.chatId = chatId;
        message.senderId = senderId;
        message.content = content;
        message.type = type;
        message.timestamp = System.currentTimeMillis();
        message.isRead = false;
        
        // Get sender name
        FirebaseRefs.users().document(senderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            message.senderName = user.name;
                        }
                    }
                    
                    // Save message
                    FirebaseRefs.messages().document(message.id).set(message.toMap())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Message sent: " + message.id);
                                
                                // Update chat's last message
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("lastMessage", content);
                                updates.put("lastMessageTime", message.timestamp);
                                updates.put("lastMessageSender", message.senderName);
                                
                                FirebaseRefs.chats().document(chatId).update(updates)
                                        .addOnSuccessListener(v -> {
                                            // Mirror to conversations collection for list view if used
                                            try { FirebaseRefs.conversations().document(chatId).set(updates, com.google.firebase.firestore.SetOptions.merge()); } catch (Exception ignored) {}
                                        })
                                        .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating chat", e);
                                            callback.onSuccess(); // Message sent successfully
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending message", e);
                                callback.onError("Failed to send message: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting sender info", e);
                    // Send message anyway
                    FirebaseRefs.messages().document(message.id).set(message.toMap())
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e2 -> callback.onError("Failed to send message: " + e2.getMessage()));
                });
    }
    
    // Get messages for a chat
    public void getChatMessages(String chatId, MessageCallback callback) {
        FirebaseRefs.messages().whereEqualTo("chatId", chatId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.Message> messages = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.Message message = doc.toObject(Models.Message.class);
                        if (message != null) {
                            message.id = doc.getId();
                            messages.add(message);
                        }
                    }
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting messages", e);
                    callback.onError("Failed to get messages: " + e.getMessage());
                });
    }

    // Listen to messages for a chat in real-time (caller must hold and remove the returned registration)
    public ListenerRegistration listenToChatMessages(String chatId, MessageCallback callback) {
        // Use only equality filter to avoid composite index requirement for quick start
        return FirebaseRefs.messages()
                .whereEqualTo("chatId", chatId)
                //.orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(200)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "listenToChatMessages error", e);
                        callback.onError("Failed to listen to messages: " + e.getMessage());
                        return;
                    }
                    if (snap == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<Models.Message> messages = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Models.Message message = doc.toObject(Models.Message.class);
                        if (message != null) {
                            message.id = doc.getId();
                            messages.add(message);
                        }
                    }
                    // Client-side sort by timestamp ascending
                    messages.sort(java.util.Comparator.comparingLong(m -> m.timestamp));
                    callback.onSuccess(messages);
                });
    }
    
    // Mark messages as read (avoid whereNotEqual to reduce index requirements)
    public void markMessagesAsRead(String chatId, String userId, ChatCallback callback) {
        FirebaseRefs.messages().whereEqualTo("chatId", chatId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    
                    // Update all unread messages that were not sent by this user
                    for (var doc : querySnapshot.getDocuments()) {
                        try {
                            Models.Message m = doc.toObject(Models.Message.class);
                            if (m != null && (m.senderId == null || !m.senderId.equals(userId))) {
                                FirebaseRefs.messages().document(doc.getId()).update("isRead", true);
                            }
                        } catch (Exception ignored) {}
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking messages as read", e);
                    callback.onError("Failed to mark messages as read: " + e.getMessage());
                });
    }
    
    // Get unread message count for a user
    public void getUnreadCount(String userId, ChatCallback callback) {
        FirebaseRefs.messages().whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int unreadCount = querySnapshot.size();
                    Log.d(TAG, "Unread messages: " + unreadCount);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread count", e);
                    callback.onError("Failed to get unread count: " + e.getMessage());
                });
    }
    
    // Add participant to group chat
    public void addParticipantToGroup(String chatId, String userId, ChatCallback callback) {
        FirebaseRefs.chats().document(chatId).update("participants", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Participant added to group: " + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding participant", e);
                    callback.onError("Failed to add participant: " + e.getMessage());
                });
    }
    
    // Remove participant from group chat
    public void removeParticipantFromGroup(String chatId, String userId, ChatCallback callback) {
        FirebaseRefs.chats().document(chatId).update("participants", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Participant removed from group: " + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing participant", e);
                    callback.onError("Failed to remove participant: " + e.getMessage());
                });
    }

    // Rename a group chat
    public void renameGroupChat(String chatId, String newName, ChatCallback callback) {
        FirebaseRefs.chats().document(chatId).update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Group renamed: " + chatId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error renaming group", e);
                    callback.onError("Failed to rename group: " + e.getMessage());
                });
    }

    // Leave a group (remove self)
    public void leaveGroup(String chatId, String userId, ChatCallback callback) {
        removeParticipantFromGroup(chatId, userId, callback);
    }

    // Listen to user's chats (real-time)
    public ListenerRegistration listenToUserChats(String userId, ChatListCallback callback) {
        return FirebaseRefs.chats().whereArrayContains("participants", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "listenToUserChats error", e);
                        callback.onError("Failed to listen to chats: " + e.getMessage());
                        return;
                    }
                    if (snap == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<Models.Chat> chats = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        Models.Chat chat = doc.toObject(Models.Chat.class);
                        if (chat != null) {
                            chat.id = doc.getId();
                            chats.add(chat);
                        }
                    }
                    callback.onSuccess(chats);
                });
    }
    
    // Delete chat
    public void deleteChat(String chatId, ChatCallback callback) {
        // Delete all messages first
        FirebaseRefs.messages().whereEqualTo("chatId", chatId).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        FirebaseRefs.messages().document(doc.getId()).delete();
                    }
                    
                    // Delete chat
                    FirebaseRefs.chats().document(chatId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Chat deleted: " + chatId);
                                // Best-effort delete any conversation doc with same id
                                try { FirebaseRefs.conversations().document(chatId).delete(); } catch (Exception ignored) {}
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting chat", e);
                                callback.onError("Failed to delete chat: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting messages", e);
                    callback.onError("Failed to delete messages: " + e.getMessage());
                });
    }
    
    // Get chat participants info
    public void getChatParticipants(String chatId, ChatCallback callback) {
        FirebaseRefs.chats().document(chatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.Chat chat = documentSnapshot.toObject(Models.Chat.class);
                        if (chat != null && chat.participants != null) {
                            // Get user details for all participants
                            for (String participantId : chat.participants) {
                                FirebaseRefs.users().document(participantId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                Models.UserProfile user = userDoc.toObject(Models.UserProfile.class);
                                                if (user != null) {
                                                    Log.d(TAG, "Participant: " + user.name + " (" + user.role + ")");
                                                }
                                            }
                                        });
                            }
                        }
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting chat participants", e);
                    callback.onError("Failed to get participants: " + e.getMessage());
                });
    }
} 