package com.example.looplab.ui.home.tabs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.ConversationsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private ConversationsAdapter adapter;
    private TextInputEditText etSearch;
    private MaterialButton btnStartAIChat, btnNewGroup, btnTeamChat, btnViewAll;
    private MaterialCardView cardAIAssistant;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout emptyState;
    private FloatingActionButton fabNewChat;
    private Toolbar toolbar;
    
    private String currentUserId;
    private String currentUserRole;
    private String searchQuery = "";
    private List<Models.Conversation> allConversations = new ArrayList<>();
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        
        initializeViews(root);
        setupToolbar();
        setupClickListeners();
        setupSearchListener();
        initializeServices();
        
        return root;
    }

    private void initializeViews(View root) {
        etSearch = root.findViewById(R.id.etSearch);
        btnStartAIChat = root.findViewById(R.id.btnStartAIChat);
        btnNewGroup = root.findViewById(R.id.btnNewGroup);
        btnTeamChat = root.findViewById(R.id.btnTeamChat);
        btnViewAll = root.findViewById(R.id.btnViewAll);
        cardAIAssistant = root.findViewById(R.id.cardAIAssistant);
        progressIndicator = root.findViewById(R.id.progressIndicator);
        emptyState = root.findViewById(R.id.emptyState);
        fabNewChat = root.findViewById(R.id.fabNewChat);
        toolbar = root.findViewById(R.id.toolbar);
        
        RecyclerView rv = root.findViewById(R.id.rvConversations);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConversationsAdapter();
        adapter.setOnConversationClickListener(conversation -> {
            if (getActivity() == null) return;
            String chatId = conversation.id;
            String chatName = conversation.name;
            android.content.Intent i = new android.content.Intent(getActivity(), com.example.looplab.ui.chat.ChatMessagesActivity.class);
            i.putExtra(com.example.looplab.ui.chat.ChatMessagesActivity.EXTRA_CHAT_ID, chatId);
            i.putExtra(com.example.looplab.ui.chat.ChatMessagesActivity.EXTRA_CHAT_NAME, chatName);
            startActivity(i);
        });
        rv.setAdapter(adapter);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("Chat");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupClickListeners() {
        btnStartAIChat.setOnClickListener(v -> startAIChat());

        btnNewGroup.setOnClickListener(v -> {
            // Create new group chat
            createNewGroup();
        });

        btnTeamChat.setOnClickListener(v -> {
            // Join team chat
            joinTeamChat();
        });

        btnViewAll.setOnClickListener(v -> {
            // View all conversations
            viewAllConversations();
        });

        fabNewChat.setOnClickListener(v -> {
            // Start new 1:1 chat
            startNewChat();
        });

        cardAIAssistant.setOnClickListener(v -> {
            // Also start AI chat when clicking the card
            startAIChat();
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterConversations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeServices() {
        loadUserRole();
        loadConversations();
    }

    private void loadUserRole() {
        if (currentUserId == null) return;
        
        FirebaseRefs.users().document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Models.UserProfile user = documentSnapshot.toObject(Models.UserProfile.class);
                        if (user != null) {
                            currentUserRole = user.role;
                            updateUIForRole();
                        }
                    }
                });
    }

    private void updateUIForRole() {
        // Show/hide features based on user role
        if ("admin".equals(currentUserRole) || "teacher".equals(currentUserRole)) {
            // Admins and teachers can create groups
            btnNewGroup.setVisibility(View.VISIBLE);
        } else {
            btnNewGroup.setVisibility(View.GONE);
        }
    }

    private void loadConversations() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        if (currentUserId == null) {
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }
            showEmptyState();
            return;
        }
        
        // Load user's conversations (map from chats collection when needed)
        // Listen to conversations if available; otherwise fall back to chats
        FirebaseRefs.conversations()
                .whereArrayContains("participants", currentUserId)
                // Avoid orderBy to reduce index requirements in quick start
                //.orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snap, e) -> {
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    
                    if (snap == null || e != null) {
                        // fallback to chats collection if conversations not present
                        loadFromChatsFallback();
                        return;
                    }
                    if (snap.isEmpty()) {
                        // no conversations documents; use chats
                        loadFromChatsFallback();
                        return;
                    }
                    
                    allConversations.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Models.Conversation conv = d.toObject(Models.Conversation.class);
                        if (conv != null) {
                            conv.id = d.getId();
                            // Ensure 1:1 chat shows other participant name if missing
                            if ("1:1".equals(conv.type) && (conv.name == null || conv.name.isEmpty())) {
                                resolveOneToOneName(conv);
                            }
                            allConversations.add(conv);
                        }
                    }
                    
                    filterConversations();
                });
    }

    private void loadFromChatsFallback() {
        if (currentUserId == null) { showEmptyState(); return; }
        com.example.looplab.data.FirebaseRefs.chats()
                .whereArrayContains("participants", currentUserId)
                // Avoid orderBy to reduce required composite indexes
                //.orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snap, ex) -> {
                    if (ex != null || snap == null) {
                        showEmptyState();
                        return;
                    }
                    allConversations.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        com.example.looplab.data.model.Models.Chat c = d.toObject(com.example.looplab.data.model.Models.Chat.class);
                        if (c != null) {
                            Models.Conversation conv = new Models.Conversation();
                            conv.id = c.id != null ? c.id : d.getId();
                            conv.name = c.name != null ? c.name : ("group".equals(c.type) ? "Group" : "");
                            conv.type = c.type;
                            conv.participants = c.participants;
                            conv.lastMessage = c.lastMessage;
                            conv.lastMessageTime = c.lastMessageTime;
                            conv.lastMessageSender = c.lastMessageSender;
                            conv.createdAt = c.createdAt;
                            conv.isActive = true;
                            if ("1:1".equals(conv.type) && (conv.name == null || conv.name.isEmpty())) {
                                resolveOneToOneName(conv);
                            }
                            allConversations.add(conv);
                        }
                    }
                    filterConversations();
                });
    }

    private void filterConversations() {
        List<Models.Conversation> filteredConversations = new ArrayList<>();
        
        for (Models.Conversation conv : allConversations) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                conv.name.toLowerCase().contains(searchQuery) ||
                (conv.lastMessage != null && conv.lastMessage.toLowerCase().contains(searchQuery));
            
            if (matchesSearch) {
                filteredConversations.add(conv);
            }
        }
        
                    // Sort by lastMessageTime desc so latest chats surface
                    java.util.Collections.sort(filteredConversations, (a,b) -> Long.compare(b.lastMessageTime, a.lastMessageTime));
        updateConversationsDisplay(filteredConversations);
    }

    private void updateConversationsDisplay(List<Models.Conversation> conversations) {
        if (conversations.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.submitList(conversations);
        }
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void startAIChat() {
        if (getActivity() == null) return;
        android.content.Intent intent = new android.content.Intent(getActivity(), com.example.looplab.ui.chat.AIChatActivity.class);
        intent.putExtra("role", currentUserRole);
        startActivity(intent);
    }

    private void createAIConversation() {
        if (currentUserId == null) return;
        
        Models.Conversation aiConversation = new Models.Conversation();
        aiConversation.name = "AI Assistant";
        aiConversation.type = "ai";
        aiConversation.participants = new ArrayList<>();
        aiConversation.participants.add(currentUserId);
        aiConversation.participants.add("ai_assistant");
        aiConversation.lastMessage = "Hello! How can I help you today?";
        aiConversation.lastMessageTime = System.currentTimeMillis();
        aiConversation.createdAt = System.currentTimeMillis();
        
        FirebaseRefs.conversations().add(aiConversation)
                .addOnSuccessListener(documentReference -> {
                    // Navigate to chat activity
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "AI Chat created!", Toast.LENGTH_SHORT).show();
                        // Intent to chat activity with conversation ID
                        // Intent intent = new Intent(getActivity(), ChatActivity.class);
                        // intent.putExtra("conversationId", documentReference.getId());
                        // startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Error creating AI chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewGroup() {
        if (getActivity() == null || currentUserId == null) return;
        // Simple new group creation with current user only; UI for selecting users can be added later
        java.util.ArrayList<String> participants = new java.util.ArrayList<>();
        participants.add(currentUserId);
        new com.example.looplab.data.ChatService().createGroupChat("New Group", participants, new com.example.looplab.data.ChatService.ChatCallback() {
            @Override
            public void onSuccess() {
                android.widget.Toast.makeText(getContext(), "Group created", android.widget.Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                android.widget.Toast.makeText(getContext(), error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinTeamChat() {
        if (getActivity() == null || currentUserId == null) return;
        // Ensure a global team conversation exists, then open AIChat as placeholder for team help
        createAIConversation();
    }

    private void viewAllConversations() {
        // No-op: conversations already listed here; hook for future screen
    }

    private void startNewChat() {
        if (getActivity() == null || currentUserId == null) return;

        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter user's email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                    new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start a chat")
                .setView(input)
                .setPositiveButton("Start", (dialog, which) -> {
                    String email = input.getText() != null ? input.getText().toString().trim() : "";
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        android.widget.Toast.makeText(getContext(), "Enter a valid email", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);

                    // Find user by email
                    com.example.looplab.data.FirebaseRefs.users()
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snap -> {
                                if (snap.isEmpty()) {
                                    if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                                    android.widget.Toast.makeText(getContext(), "No user found for this email", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                com.google.firebase.firestore.DocumentSnapshot doc = snap.getDocuments().get(0);
                                com.example.looplab.data.model.Models.UserProfile user = doc.toObject(com.example.looplab.data.model.Models.UserProfile.class);
                                String targetUid = user != null ? user.uid : doc.getId();
                                String targetName = user != null && user.name != null ? user.name : email;

                                if (currentUserId.equals(targetUid)) {
                                    if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                                    android.widget.Toast.makeText(getContext(), "You cannot chat with yourself", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                com.example.looplab.data.ChatService chatService = new com.example.looplab.data.ChatService();
                                chatService.getOrCreateOneToOneChat(currentUserId, targetUid, new com.example.looplab.data.ChatService.ChatIdCallback() {
            @Override
                                    public void onSuccess(String chatId) {
                                        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                                        // Ensure a chat name helpful for 1:1
                                        com.example.looplab.data.FirebaseRefs.chats().document(chatId).update("name", targetName);

                                        // Auto-starter message if empty thread
                                        com.example.looplab.data.FirebaseRefs.messages()
                                                .whereEqualTo("chatId", chatId)
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener((QuerySnapshot msnap) -> {
                                                    if (msnap == null || msnap.isEmpty()) {
                                                        String starter = "Say hi to " + targetName + "!";
                                                        // send as system type so it renders centered/neutral
                                                        chatService.sendMessage(chatId, "system", starter, "system", new com.example.looplab.data.ChatService.ChatCallback() {
                                                            @Override public void onSuccess() { openChat(chatId, targetName); }
                                                            @Override public void onError(String error) { openChat(chatId, targetName); }
                                                        });
                                                    } else {
                                                        openChat(chatId, targetName);
                                                    }
                                                })
                                                .addOnFailureListener(ex -> openChat(chatId, targetName));
            }

            @Override
            public void onError(String error) {
                                        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(), error, android.widget.Toast.LENGTH_SHORT).show();
            }
                                });
                            })
                            .addOnFailureListener(e -> {
                                if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                                android.widget.Toast.makeText(getContext(), "Error searching user: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openChat(String chatId, String chatName) {
        if (getActivity() == null) return;
        android.content.Intent i = new android.content.Intent(getActivity(), com.example.looplab.ui.chat.ChatMessagesActivity.class);
        i.putExtra(com.example.looplab.ui.chat.ChatMessagesActivity.EXTRA_CHAT_ID, chatId);
        i.putExtra(com.example.looplab.ui.chat.ChatMessagesActivity.EXTRA_CHAT_NAME, chatName);
        startActivity(i);
    }

    // Resolve display name for 1:1 chats where name is missing
    private void resolveOneToOneName(Models.Conversation conv) {
        if (conv == null || conv.participants == null || conv.participants.size() != 2) return;
        if (conv.name != null && !conv.name.isEmpty()) return;
        String otherId = conv.participants.get(0).equals(currentUserId) ? conv.participants.get(1) : conv.participants.get(0);
        FirebaseRefs.users().document(otherId).get()
                .addOnSuccessListener(doc -> {
                    Models.UserProfile u = doc.toObject(Models.UserProfile.class);
                    String display = (u != null && u.name != null && !u.name.isEmpty()) ? u.name : "Chat";
                    conv.name = display;
                    // Trigger UI refresh
                    filterConversations();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserRole();
        loadConversations();
    }
}


