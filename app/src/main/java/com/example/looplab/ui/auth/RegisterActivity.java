package com.example.looplab.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.auth.role.RoleSelectionActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        auth = FirebaseAuth.getInstance();

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            EditText etName = findViewById(R.id.etName);
            EditText etEmail = findViewById(R.id.etEmail);
            EditText etPassword = findViewById(R.id.etPassword);
            EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
            CheckBox cbTerms = findViewById(R.id.cbTerms);
            
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();
            
            // Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create account
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        Models.UserProfile profile = new Models.UserProfile();
                        profile.uid = uid;
                        profile.name = name;
                        profile.email = email;
                        profile.role = ""; // Empty role - will be set in role selection
                        profile.createdAt = System.currentTimeMillis();
                        profile.isFirstTime = true;
                        
                        FirebaseRefs.users().document(uid).set(profile.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                    // Go to role selection for first-time setup
                                    Intent intent = new Intent(RegisterActivity.this, RoleSelectionActivity.class);
                                    intent.putExtra("isFirstTime", true);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("email")) {
                            Toast.makeText(this, "Email already exists. Please sign in instead.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Handle login link
        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}


