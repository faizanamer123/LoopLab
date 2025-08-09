package com.example.looplab.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.AIChatbotService;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {
    private static final String TAG = "AIChatActivity";

    private AIChatbotService chatbotService;
    private RecyclerView recyclerView;
    private MessagesAdapter adapter;
    private EditText input;
    private ImageButton send;
    private ProgressBar typing;
    private TextView configHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            EdgeToEdge.enable(this);
            Log.d(TAG, "EdgeToEdge enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable EdgeToEdge", e);
        }
        
        try {
            Log.d(TAG, "Attempting to inflate layout: activity_ai_chat");
            setContentView(R.layout.activity_ai_chat);
            Log.d(TAG, "Layout inflated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout", e);
            // Show error message and finish activity
            TextView errorView = new TextView(this);
            errorView.setText("Failed to load AI Chat. Error: " + e.getMessage());
            errorView.setPadding(50, 50, 50, 50);
            errorView.setTextSize(16);
            setContentView(errorView);
            
            // Add a button to retry
            Button retryButton = new Button(this);
            retryButton.setText("Retry");
            retryButton.setOnClickListener(v -> {
                try {
                    setContentView(R.layout.activity_ai_chat);
                    initializeViews();
                    setupChatbot();
                    setupRecyclerView();
                    loadConversationHistory();
                    setupClickListeners();
                } catch (Exception retryException) {
                    Log.e(TAG, "Retry failed", retryException);
                    Toast.makeText(this, "Retry failed: " + retryException.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            
            LinearLayout errorContainer = new LinearLayout(this);
            errorContainer.setOrientation(LinearLayout.VERTICAL);
            errorContainer.setGravity(android.view.Gravity.CENTER);
            errorContainer.addView(errorView);
            errorContainer.addView(retryButton);
            setContentView(errorContainer);
            return;
        }

        try {
            Log.d(TAG, "Starting view initialization");
            initializeViews();
            Log.d(TAG, "Views initialized successfully");
            
            Log.d(TAG, "Setting up chatbot");
            setupChatbot();
            Log.d(TAG, "Chatbot setup completed");
            
            Log.d(TAG, "Setting up RecyclerView");
            setupRecyclerView();
            Log.d(TAG, "RecyclerView setup completed");
            
            Log.d(TAG, "Loading conversation history");
            loadConversationHistory();
            Log.d(TAG, "Conversation history loaded");
            
            Log.d(TAG, "Setting up click listeners");
            setupClickListeners();
            Log.d(TAG, "Click listeners setup completed");
            
            Log.d(TAG, "AIChatActivity initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization", e);
            try {
                Snackbar.make(findViewById(android.R.id.content), 
                    "Error initializing AI Chat: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
            } catch (Exception snackbarException) {
                Log.e(TAG, "Failed to show error snackbar", snackbarException);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Refresh configuration status
            if (chatbotService != null && configHint != null) {
                if (!chatbotService.isConfigured()) {
                    configHint.setVisibility(View.VISIBLE);
                } else {
                    configHint.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Clean up any resources if needed
            if (chatbotService != null) {
                // Service cleanup if needed
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rvMessages);
        input = findViewById(R.id.etMessage);
        send = findViewById(R.id.btnSend);
        typing = findViewById(R.id.progressTyping);
        configHint = findViewById(R.id.tvConfigHint);
        
        if (recyclerView == null || input == null || send == null || typing == null || configHint == null) {
            throw new IllegalStateException("One or more views not found in layout");
        }
    }

    private void setupChatbot() {
        chatbotService = new AIChatbotService(this);
        
        if (!chatbotService.isConfigured()) {
            configHint.setVisibility(View.VISIBLE);
            Log.w(TAG, "AI chatbot not configured");
        } else {
            configHint.setVisibility(View.GONE);
            Log.d(TAG, "AI chatbot configured successfully");
        }
    }

    private void setupRecyclerView() {
        adapter = new MessagesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadConversationHistory() {
        try {
            List<AIChatbotService.ChatMessage> history = chatbotService.getConversationHistory();
            for (AIChatbotService.ChatMessage m : history) {
                adapter.add(m.role, m.content);
            }
            Log.d(TAG, "Loaded " + history.size() + " conversation history items");
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversation history", e);
        }
    }

    private void setupClickListeners() {
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            
            try {
                // Add user message to UI
                adapter.add("user", text);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                input.setText("");
                
                // Get user role from intent
                String userRole = getIntent().getStringExtra("role");
                if (userRole == null) {
                    userRole = "student"; // Default role
                }
                
                // Send to AI service
                chatbotService.sendMessage(text, userRole, new AIChatbotService.ChatbotCallback() {
                    @Override
                    public void onResponse(String response) {
                        runOnUiThread(() -> {
                            adapter.add("assistant", response);
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Snackbar.make(recyclerView, error, Snackbar.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onTypingStart() {
                        runOnUiThread(() -> typing.setVisibility(View.VISIBLE));
                    }

                    @Override
                    public void onTypingEnd() {
                        runOnUiThread(() -> typing.setVisibility(View.GONE));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                Snackbar.make(recyclerView, "Error sending message: " + e.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
            }
        });
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesViewHolder> {
        private final List<AIChatbotService.ChatMessage> data = new ArrayList<>();

        void add(String role, String content) {
            data.add(new AIChatbotService.ChatMessage(role, content));
            notifyItemInserted(data.size() - 1);
        }

        @Override
        public MessagesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            try {
                View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ai_message, parent, false);
                return new MessagesViewHolder(v);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating message item", e);
                // Return a simple text view as fallback
                TextView fallbackView = new TextView(parent.getContext());
                fallbackView.setPadding(20, 20, 20, 20);
                return new MessagesViewHolder(fallbackView);
            }
        }

        @Override
        public void onBindViewHolder(MessagesViewHolder holder, int position) {
            try {
                AIChatbotService.ChatMessage m = data.get(position);
                holder.bind(m.role, m.content);
            } catch (Exception e) {
                Log.e(TAG, "Error binding message at position " + position, e);
            }
        }

        @Override
        public int getItemCount() { 
            return data.size(); 
        }
    }

    static class MessagesViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUser;
        private final TextView tvAssistant;
        
        MessagesViewHolder(View itemView) {
            super(itemView);
            
            TextView userView = null;
            TextView assistantView = null;
            
            // Check if this is a fallback view
            if (itemView instanceof TextView) {
                // Fallback view - use the same view for both
                userView = (TextView) itemView;
                assistantView = (TextView) itemView;
            } else {
                try {
                    userView = itemView.findViewById(R.id.tvUserMsg);
                    assistantView = itemView.findViewById(R.id.tvAssistantMsg);
                    
                    if (userView == null || assistantView == null) {
                        throw new IllegalStateException("Message views not found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error finding message views", e);
                    // Create dummy views as fallback
                    userView = new TextView(itemView.getContext());
                    assistantView = new TextView(itemView.getContext());
                }
            }
            
            // Assign to final variables only once
            tvUser = userView;
            tvAssistant = assistantView;
        }
        
        void bind(String role, String content) {
            try {
                if ("user".equals(role)) {
                    if (tvUser != null) {
                        tvUser.setVisibility(View.VISIBLE);
                        tvUser.setText(content);
                    }
                    if (tvAssistant != null) {
                        tvAssistant.setVisibility(View.GONE);
                    }
                } else {
                    if (tvAssistant != null) {
                        tvAssistant.setVisibility(View.VISIBLE);
                        tvAssistant.setText(content);
                    }
                    if (tvUser != null) {
                        tvUser.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding message content", e);
            }
        }
    }
}


