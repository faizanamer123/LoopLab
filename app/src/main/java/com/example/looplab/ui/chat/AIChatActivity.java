package com.example.looplab.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.AIChatbotService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AIChatActivity extends AppCompatActivity {
    private static final String TAG = "AIChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private MaterialButton btnSend;
    private LinearLayout welcomeContainer;
    private LinearLayout typingIndicator;
    private TextView tvCharCount;
    private TextView tvConfigHint;

    private MessagesAdapter adapter;
    private AIChatbotService aiService;
    private List<Message> messages = new ArrayList<>();
    private boolean isTyping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
        setContentView(R.layout.activity_ai_chat);

            // Initialize AI service
            aiService = new AIChatbotService();
            
            // Initialize views
            initializeViews();
            setupToolbar();
            setupMessageInput();
            setupRecyclerView();
            
            // Check if AI is configured
            checkAIConfiguration();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showError("Failed to initialize chat", e.getMessage());
        }
    }

    private void initializeViews() {
        try {
            rvMessages = findViewById(R.id.rvMessages);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);
            welcomeContainer = findViewById(R.id.welcomeContainer);
            typingIndicator = findViewById(R.id.typingIndicator);
            tvCharCount = findViewById(R.id.tvCharCount);
            tvConfigHint = findViewById(R.id.tvConfigHint);

            if (rvMessages == null || etMessage == null || btnSend == null) {
                throw new IllegalStateException("Required views not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw new RuntimeException("Failed to initialize views", e);
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void setupMessageInput() {
        try {
            // Character count
            etMessage.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int charCount = s.length();
                    tvCharCount.setText(charCount + "/4000");
                    tvCharCount.setVisibility(charCount > 0 ? View.VISIBLE : View.GONE);
                    
                    // Enable/disable send button
                    btnSend.setEnabled(charCount > 0 && !isTyping);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Send button click
            btnSend.setOnClickListener(v -> {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty() && !isTyping) {
                    sendMessage(message);
                }
            });

            // Enter key to send
            etMessage.setOnEditorActionListener((v, actionId, event) -> {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty() && !isTyping) {
                    sendMessage(message);
                    return true;
                }
                return false;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up message input", e);
        }
    }

    private void setupRecyclerView() {
        try {
            adapter = new MessagesAdapter();
            rvMessages.setLayoutManager(new LinearLayoutManager(this));
            rvMessages.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void checkAIConfiguration() {
        try {
            String apiKey = aiService.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                tvConfigHint.setVisibility(View.VISIBLE);
                welcomeContainer.setVisibility(View.GONE);
                btnSend.setEnabled(false);
                etMessage.setEnabled(false);
            } else {
                tvConfigHint.setVisibility(View.GONE);
                welcomeContainer.setVisibility(View.VISIBLE);
                btnSend.setEnabled(true);
                etMessage.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking AI configuration", e);
            tvConfigHint.setVisibility(View.VISIBLE);
        }
    }

    private void sendMessage(String messageText) {
        try {
            if (isTyping) return;

            // Add user message
            Message userMessage = new Message(Message.TYPE_USER, messageText, new Date());
            addMessage(userMessage);

            // Clear input
            etMessage.setText("");
            tvCharCount.setVisibility(View.GONE);

            // Show typing indicator
            showTypingIndicator(true);

            // Send to AI
            aiService.sendMessage(messageText, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "AI request failed", e);
                    runOnUiThread(() -> {
                        showTypingIndicator(false);
                        showError("Failed to get AI response", e.getMessage());
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            String aiResponse = parseAIResponse(responseBody);
                            
                            runOnUiThread(() -> {
                                showTypingIndicator(false);
                                if (aiResponse != null) {
                                    Message aiMessage = new Message(Message.TYPE_AI, aiResponse, new Date());
                                    addMessage(aiMessage);
                                } else {
                                    showError("Invalid AI response", "Could not parse AI response");
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                showTypingIndicator(false);
                                showError("AI request failed", "HTTP " + response.code());
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing AI response", e);
                        runOnUiThread(() -> {
                            showTypingIndicator(false);
                            showError("Error parsing response", e.getMessage());
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            showTypingIndicator(false);
            showError("Failed to send message", e.getMessage());
        }
    }

    private String parseAIResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    JSONObject part = parts.getJSONObject(0);
                    return part.getString("text");
                }
            }
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response", e);
            return null;
        }
    }

    private void addMessage(Message message) {
        try {
            messages.add(message);
            adapter.notifyItemInserted(messages.size() - 1);
            
            // Hide welcome container when first message is added
            if (messages.size() == 1) {
                welcomeContainer.setVisibility(View.GONE);
                rvMessages.setVisibility(View.VISIBLE);
            }
            
            // Scroll to bottom
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(messages.size() - 1));
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding message", e);
        }
    }

    private void showTypingIndicator(boolean show) {
        try {
            isTyping = show;
            typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!show && etMessage.getText().length() > 0);
            
            if (show) {
                rvMessages.post(() -> rvMessages.smoothScrollToPosition(messages.size()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing typing indicator", e);
        }
    }

    private void showError(String title, String message) {
        try {
            Snackbar.make(findViewById(android.R.id.content), title + ": " + message, Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
            Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
        }
    }

    // Message data class
    public static class Message {
        public static final int TYPE_USER = 1;
        public static final int TYPE_AI = 2;
        public static final int TYPE_SYSTEM = 3;

        public final int type;
        public final String text;
        public final Date timestamp;

        public Message(int type, String text, Date timestamp) {
            this.type = type;
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    // Adapter for messages
    private class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_ai_message, parent, false);
                return new MessageViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error creating view holder", e);
                // Return a fallback view
                TextView fallbackView = new TextView(parent.getContext());
                fallbackView.setText("Error loading message");
                return new MessageViewHolder(fallbackView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            try {
                Message message = messages.get(position);
                holder.bind(message);
            } catch (Exception e) {
                Log.e(TAG, "Error binding view holder", e);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            private final LinearLayout userMessageContainer;
            private final LinearLayout aiMessageContainer;
            private final LinearLayout systemMessageContainer;
            private final TextView tvUserMessage;
            private final TextView tvAIMessage;
            private final TextView tvSystemMessage;
            private final TextView tvUserTime;
            private final TextView tvAITime;

            MessageViewHolder(View itemView) {
            super(itemView);

                // Initialize views
                userMessageContainer = itemView.findViewById(R.id.userMessageContainer);
                aiMessageContainer = itemView.findViewById(R.id.aiMessageContainer);
                systemMessageContainer = itemView.findViewById(R.id.systemMessageContainer);
                tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
                tvAIMessage = itemView.findViewById(R.id.tvAIMessage);
                tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
                tvUserTime = itemView.findViewById(R.id.tvUserTime);
                tvAITime = itemView.findViewById(R.id.tvAITime);

                // Handle fallback case
                if (userMessageContainer == null || aiMessageContainer == null) {
                    Log.w(TAG, "Message view containers not found, using fallback");
                    return;
                }
            }

            void bind(Message message) {
                try {
                    // Hide all containers first
                    if (userMessageContainer != null) userMessageContainer.setVisibility(View.GONE);
                    if (aiMessageContainer != null) aiMessageContainer.setVisibility(View.GONE);
                    if (systemMessageContainer != null) systemMessageContainer.setVisibility(View.GONE);

                    // Format timestamp
                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    String timeStr = sdf.format(message.timestamp);

                    switch (message.type) {
                        case Message.TYPE_USER:
                            if (userMessageContainer != null && tvUserMessage != null && tvUserTime != null) {
                                userMessageContainer.setVisibility(View.VISIBLE);
                                tvUserMessage.setText(message.text);
                                tvUserTime.setText(timeStr);
                            }
                            break;

                        case Message.TYPE_AI:
                            if (aiMessageContainer != null && tvAIMessage != null && tvAITime != null) {
                                aiMessageContainer.setVisibility(View.VISIBLE);
                                tvAIMessage.setText(message.text);
                                tvAITime.setText(timeStr);
                            }
                            break;

                        case Message.TYPE_SYSTEM:
                            if (systemMessageContainer != null && tvSystemMessage != null) {
                                systemMessageContainer.setVisibility(View.VISIBLE);
                                tvSystemMessage.setText(message.text);
                            }
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error binding message", e);
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh configuration check
        checkAIConfiguration();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }
}


