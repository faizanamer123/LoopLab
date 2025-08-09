package com.example.looplab.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.looplab.R;
import com.example.looplab.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView ivProfilePicture;
    private TextView tvUserName, tvUserEmail;
    private SwitchMaterial switchNotifications, switchDarkMode;
    
    private SharedPreferences preferences;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupToolbar();
        loadUserData();
        setupClickListeners();
        setupPreferences();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        
        auth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("LoopLabSettings", MODE_PRIVATE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            tvUserName.setText(currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "User");
            tvUserEmail.setText(currentUser.getEmail());
            
            // TODO: Load profile picture from Firebase Storage or URL
            // For now, using default profile icon
        }
    }

    private void setupPreferences() {
        // Load saved preferences
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
        boolean darkModeEnabled = preferences.getBoolean("dark_mode_enabled", false);
        
        switchNotifications.setChecked(notificationsEnabled);
        switchDarkMode.setChecked(darkModeEnabled);
    }

    private void setupClickListeners() {
        // Profile card click
        findViewById(R.id.ivProfilePicture).setOnClickListener(v -> {
            // Open Profile tab
            finish();
        });

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("notifications_enabled", isChecked).apply();
            Toast.makeText(this, "Notifications " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("dark_mode_enabled", isChecked).apply();
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            
            // Show feedback to user
            String themeName = isChecked ? "Dark" : "Light";
            Toast.makeText(this, "Switched to " + themeName + " theme", Toast.LENGTH_SHORT).show();
        });

        // Language setting
        findViewById(R.id.settingLanguage).setOnClickListener(v -> {
            showLanguageDialog();
        });

        // Edit Profile
        findViewById(R.id.settingEditProfile).setOnClickListener(v -> {
            // Not implemented: return to profile
            finish();
        });

        // Change Password
        findViewById(R.id.settingChangePassword).setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        // Logout
        findViewById(R.id.settingLogout).setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Chinese"};
        int currentSelection = preferences.getInt("language_selection", 0);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentSelection, (dialog, which) -> {
                preferences.edit().putInt("language_selection", which).apply();
                Toast.makeText(this, "Language changed to " + languages[which], 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showChangePasswordDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setMessage("A password reset email will be sent to " + currentUser.getEmail())
                .setPositiveButton("Send Email", (dialog, which) -> {
                    auth.sendPasswordResetEmail(currentUser.getEmail())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Password reset email sent", 
                                Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to send reset email: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                logout();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void logout() {
        auth.signOut();
        
        // Clear any cached data
        preferences.edit().clear().apply();
        
        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}
