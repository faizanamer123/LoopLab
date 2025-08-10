package com.example.looplab.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class LiveSessionActivity extends AppCompatActivity {

    private TextInputEditText etSessionTitle, etSessionDescription;
    private MaterialButton btnStartSession, btnJoinSession;
    private String currentUserId;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live_session);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etSessionTitle = findViewById(R.id.etSessionTitle);
        etSessionDescription = findViewById(R.id.etSessionDescription);
        btnStartSession = findViewById(R.id.btnStartSession);
        btnJoinSession = findViewById(R.id.btnJoinSession);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnStartSession.setOnClickListener(v -> {
            if (validateInputs()) {
                startLiveSession();
            }
        });

        btnJoinSession.setOnClickListener(v -> {
            // Show dialog to enter session code
            showJoinSessionDialog();
        });
    }

    private boolean validateInputs() {
        String title = etSessionTitle.getText().toString().trim();
        String description = etSessionDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etSessionTitle.setError("Session title is required");
            return false;
        }

        if (description.isEmpty()) {
            etSessionDescription.setError("Session description is required");
            return false;
        }

        return true;
    }

    private void startLiveSession() {
        String title = etSessionTitle.getText().toString().trim();
        String description = etSessionDescription.getText().toString().trim();
        
        // Generate unique session ID
        String sessionId = generateSessionId();
        
        try {
            // Configure Jitsi Meet options
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(sessionId)
                    .setAudioMuted(false)
                    .setVideoMuted(false)
                    .build();

            // Launch Jitsi Meet activity
            JitsiMeetActivity.launch(this, options);
            
            // Save session to database
            saveSessionToDatabase(title, description, sessionId);
            
        } catch (MalformedURLException e) {
            Toast.makeText(this, "Error starting session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showJoinSessionDialog() {
        // Create a simple dialog to enter session code
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Join Live Session");
        builder.setMessage("Enter the session code to join:");
        
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Session Code");
        builder.setView(input);
        
        builder.setPositiveButton("Join", (dialog, which) -> {
            String sessionCode = input.getText().toString().trim();
            if (!sessionCode.isEmpty()) {
                joinLiveSession(sessionCode);
            } else {
                Toast.makeText(this, "Please enter a session code", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void joinLiveSession(String sessionCode) {
        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(sessionCode)
                    .setAudioMuted(false)
                    .setVideoMuted(false)
                    .build();

            JitsiMeetActivity.launch(this, options);
            
        } catch (MalformedURLException e) {
            Toast.makeText(this, "Error joining session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generateSessionId() {
        return "looplab-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String getCurrentUserName() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "User";
    }

    private String getCurrentUserEmail() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
    }

    private void saveSessionToDatabase(String title, String description, String sessionId) {
        try {
            com.google.firebase.firestore.DocumentReference ref = com.example.looplab.data.FirebaseRefs.liveSessions().document(sessionId);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", sessionId);
            data.put("title", title);
            data.put("description", description);
            data.put("instructorId", currentUserId);
            data.put("instructorName", getCurrentUserName());
            data.put("createdAt", System.currentTimeMillis());
            data.put("isActive", true);
            ref.set(data);
        } catch (Exception ignored) {
        }
    }
}


