package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

public class AddEditUserActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etName, etEmail, etPassword;
    private ChipGroup chipGroupRole;
    private SwitchMaterial switchActive, switchVerified;
    private MaterialButton btnSave, btnCancel;
    private CircularProgressIndicator progressIndicator;

    private boolean isEdit = false;
    private String userId = null;
    private Models.UserProfile currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_user);

        isEdit = getIntent().getBooleanExtra("isEdit", false);
        userId = getIntent().getStringExtra("userId");

        initializeViews();
        setupToolbar();
        setupClickListeners();

        if (isEdit && userId != null) {
            loadUserData();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        chipGroupRole = findViewById(R.id.chipGroupRole);
        switchActive = findViewById(R.id.switchActive);
        switchVerified = findViewById(R.id.switchVerified);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEdit ? "Edit User" : "Add New User");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveUser());
        btnCancel.setOnClickListener(v -> finish());

        // If editing, hide password field
        if (isEdit) {
            etPassword.setVisibility(View.GONE);
            findViewById(R.id.tilPassword).setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        FirebaseRefs.users().document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressIndicator.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(Models.UserProfile.class);
                        if (currentUser != null) {
                            populateFields();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        etName.setText(currentUser.name);
        etEmail.setText(currentUser.email);
        switchActive.setChecked(currentUser.isActive);
        switchVerified.setChecked(currentUser.verified);

        // Set role
        for (int i = 0; i < chipGroupRole.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupRole.getChildAt(i);
            if (chip.getTag().equals(currentUser.role)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void saveUser() {
        if (!validateInput()) {
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (isEdit) {
            updateUser();
        } else {
            createUser();
        }
    }

    private boolean validateInput() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }

        if (!email.contains("@")) {
            etEmail.setError("Invalid email format");
            return false;
        }

        if (!isEdit && password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        if (chipGroupRole.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String selectedRole = getSelectedRole();

        // Create user with Firebase Auth
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // Create user profile
                    Models.UserProfile newUser = new Models.UserProfile();
                    newUser.uid = uid;
                    newUser.name = name;
                    newUser.email = email;
                    newUser.role = selectedRole;
                    newUser.isActive = switchActive.isChecked();
                    newUser.verified = switchVerified.isChecked();
                    newUser.createdAt = System.currentTimeMillis();
                    newUser.lastActive = System.currentTimeMillis();
                    newUser.points = 0;
                    newUser.badges = new ArrayList<>();

                    // Save to Firestore
                    FirebaseRefs.users().document(uid).set(newUser.toMap())
                            .addOnSuccessListener(aVoid -> {
                                progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressIndicator.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                                Toast.makeText(this, "Failed to save user profile: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Failed to create user: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String selectedRole = getSelectedRole();

        // Update user profile
        FirebaseRefs.users().document(userId)
                .update(
                        "name", name,
                        "email", email,
                        "role", selectedRole,
                        "isActive", switchActive.isChecked(),
                        "verified", switchVerified.isChecked()
                )
                .addOnSuccessListener(aVoid -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Failed to update user: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedRole() {
        int checkedId = chipGroupRole.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = findViewById(checkedId);
            return chip.getTag().toString();
        }
        return "student"; // default
    }
}
