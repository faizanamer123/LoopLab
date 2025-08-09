package com.example.looplab.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChatbotService {
    private static final String TAG = "AIChatbotService";
    // Use Gemini 2.0 Flash Lite model per request
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";
    // Default API key provided by user (note: embedding keys in code is insecure; prefer runtime config)
    private static final String DEFAULT_API_KEY = "AIzaSyBbSU4lHfiteSkXl8TgU7Ls3Jg0EjKNWLk";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final SharedPreferences prefs;
    private final Handler mainHandler;
    private final List<ChatMessage> conversationHistory;

    // Chat message model for conversation history
    public static class ChatMessage {
        public String role;
        public String content;
        public long timestamp;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public AIChatbotService(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences("looplab_prefs", Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.conversationHistory = new ArrayList<>();

        // Ensure API key configured
        ensureDefaultApiKey();

        // Load conversation history on initialization
        loadConversationHistory();
    }

    public interface ChatbotCallback {
        void onResponse(String response);
        void onError(String error);
        void onTypingStart();
        void onTypingEnd();
    }

    public void sendMessage(String message, String userRole, ChatbotCallback callback) {
        if (message == null || message.trim().isEmpty()) {
            runOnMainThread(() -> callback.onError("Please enter a message"));
            return;
        }

        // Get API key dynamically
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            runOnMainThread(() -> callback.onError("AI chatbot not configured. Please contact admin."));
            return;
        }

        // Notify typing started
        runOnMainThread(callback::onTypingStart);

        // Add user message to conversation history
        addMessageToHistory("user", message);

        // Create dynamic system prompt
        String systemPrompt = createDynamicSystemPrompt(userRole);

        // Create request body with conversation context
        JsonObject requestBody = createRequestBody(systemPrompt, message);

        // Execute request asynchronously
        executeRequest(requestBody, apiKey, callback);
    }

    private void executeRequest(JsonObject requestBody, String apiKey, ChatbotCallback callback) {
        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                        .url(GEMINI_API_URL + "?key=" + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "LoopLab-Android-App/1.0")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnMainThread(callback::onTypingEnd);

                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        handleSuccessfulResponse(responseBody, callback);
                    } else {
                        final String errorMsg = handleErrorResponse(response);
                        runOnMainThread(() -> callback.onError(errorMsg));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error calling Gemini API", e);
                runOnMainThread(() -> {
                    callback.onTypingEnd();
                    callback.onError("Network error: " + e.getMessage());
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                runOnMainThread(() -> {
                    callback.onTypingEnd();
                    callback.onError("Unexpected error occurred");
                });
            }
        }).start();
    }

    private void handleSuccessfulResponse(String responseBody, ChatbotCallback callback) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();

                    // Check if response was blocked
                    if (candidate.has("finishReason")) {
                        String finishReason = candidate.get("finishReason").getAsString();
                        if ("SAFETY".equals(finishReason)) {
                            runOnMainThread(() -> callback.onError("Response blocked due to safety policies"));
                            return;
                        }
                    }

                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                if (part.has("text")) {
                                    String aiResponse = part.get("text").getAsString().trim();

                                    // Add AI response to conversation history
                                    addMessageToHistory("assistant", aiResponse);

                                    runOnMainThread(() -> callback.onResponse(aiResponse));
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            runOnMainThread(() -> callback.onError("Invalid response format from AI service"));

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Failed to parse API response", e);
            runOnMainThread(() -> callback.onError("Failed to parse AI response"));
        }
    }

    private JsonObject createRequestBody(String systemPrompt, String userMessage) {
        JsonObject requestBody = new JsonObject();

        // Create contents array with conversation history
        JsonArray contents = new JsonArray();

        // Add system message
        JsonObject systemContent = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt);
        systemParts.add(systemPart);
        systemContent.add("parts", systemParts);
        systemContent.addProperty("role", "user");
        contents.add(systemContent);

        // Add conversation history (last 10 messages for context)
        int historyStart = Math.max(0, conversationHistory.size() - 10);
        for (int i = historyStart; i < conversationHistory.size(); i++) {
            ChatMessage msg = conversationHistory.get(i);
            JsonObject msgContent = new JsonObject();
            JsonArray msgParts = new JsonArray();
            JsonObject msgPart = new JsonObject();
            msgPart.addProperty("text", msg.content);
            msgParts.add(msgPart);
            msgContent.add("parts", msgParts);
            msgContent.addProperty("role", msg.role.equals("user") ? "user" : "model");
            contents.add(msgContent);
        }

        requestBody.add("contents", contents);

        // Dynamic generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 1000);
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 40);
        requestBody.add("generationConfig", generationConfig);

        // Safety settings
        JsonArray safetySettings = new JsonArray();
        String[] categories = {"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"};
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.add(setting);
        }
        requestBody.add("safetySettings", safetySettings);

        return requestBody;
    }

    private String createDynamicSystemPrompt(String userRole) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are LoopLab AI Assistant, a helpful and knowledgeable AI assistant for the LoopLab learning platform. ");
        prompt.append("LoopLab is a student-led tech community that empowers learners through:");
        prompt.append("\n• Engaging video content and tutorials");
        prompt.append("\n• Interactive live sessions and workshops");
        prompt.append("\n• Vibrant tech events and competitions");
        prompt.append("\n• Collaborative community features and discussions");
        prompt.append("\n• Peer-to-peer learning and mentorship");

        prompt.append("\n\nCurrent user role: ").append(userRole != null ? userRole : "Student");
        prompt.append("\n\nYou can assist with:");

        // Dynamic capabilities based on user role
        if ("admin".equalsIgnoreCase(userRole)) {
            prompt.append("\n• Platform administration and management");
            prompt.append("\n• User management and moderation");
            prompt.append("\n• Analytics and reporting insights");
        } else if ("teacher".equalsIgnoreCase(userRole) || "instructor".equalsIgnoreCase(userRole)) {
            prompt.append("\n• Course creation and content management");
            prompt.append("\n• Student progress tracking and assessment");
            prompt.append("\n• Live session scheduling and management");
        }

        // Common capabilities
        prompt.append("\n• App navigation and feature explanations");
        prompt.append("\n• Course enrollment and progress tracking");
        prompt.append("\n• Event registration and details");
        prompt.append("\n• Technical support and troubleshooting");
        prompt.append("\n• Personalized learning recommendations");
        prompt.append("\n• Community guidelines and best practices");
        prompt.append("\n• General questions about LoopLab features");

        prompt.append("\n\nGuidelines:");
        prompt.append("\n• Keep responses concise, friendly, and encouraging");
        prompt.append("\n• Provide step-by-step instructions when needed");
        prompt.append("\n• Be supportive of learning goals and challenges");
        prompt.append("\n• If unsure about specific app details, suggest checking help section or contacting support");
        prompt.append("\n• Maintain a professional yet approachable tone");
        prompt.append("\n• Focus on empowering users to achieve their learning objectives");

        return prompt.toString();
    }

    private void addMessageToHistory(String role, String content) {
        conversationHistory.add(new ChatMessage(role, content));
        saveConversationHistory();

        // Keep history manageable (last 50 messages)
        if (conversationHistory.size() > 50) {
            conversationHistory.subList(0, conversationHistory.size() - 50).clear();
        }
    }

    private void saveConversationHistory() {
        try {
            String historyJson = gson.toJson(conversationHistory);
            prefs.edit().putString("chat_history", historyJson).apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to save conversation history", e);
        }
    }

    private void loadConversationHistory() {
        try {
            String historyJson = prefs.getString("chat_history", "[]");
            ChatMessage[] messages = gson.fromJson(historyJson, ChatMessage[].class);
            if (messages != null) {
                for (ChatMessage msg : messages) {
                    conversationHistory.add(msg);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load conversation history", e);
            conversationHistory.clear();
        }
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private String getApiKey() {
        return prefs.getString("gemini_api_key", "");
    }

    // Public methods for configuration and management
    public void setApiKey(String apiKey) {
        prefs.edit().putString("gemini_api_key", apiKey != null ? apiKey.trim() : "").apply();
    }

    public boolean isConfigured() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    private void ensureDefaultApiKey() {
        if (!isConfigured() && DEFAULT_API_KEY != null && !DEFAULT_API_KEY.isEmpty()) {
            setApiKey(DEFAULT_API_KEY);
        }
    }

    public void clearConversationHistory() {
        conversationHistory.clear();
        prefs.edit().remove("chat_history").apply();
    }

    public List<ChatMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    public void updateConfiguration(double temperature, int maxTokens) {
        prefs.edit()
                .putFloat("ai_temperature", (float) temperature)
                .putInt("ai_max_tokens", maxTokens)
                .apply();
    }

    private String handleErrorResponse(Response response) {
        String errorMsg = "HTTP Error: " + response.code();
        if (response.body() != null) {
            try {
                String errorBody = response.body().string();
                JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                if (errorJson.has("error")) {
                    JsonObject error = errorJson.getAsJsonObject("error");
                    errorMsg = error.has("message") ?
                            error.get("message").getAsString() :
                            "API Error: " + response.code();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse error response", e);
            }
        }
        return errorMsg;
    }
}