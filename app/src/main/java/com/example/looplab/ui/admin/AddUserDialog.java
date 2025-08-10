package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddUserDialog extends DialogFragment {

    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spinnerRole;
    private MaterialButton btnAddUser, btnCancel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_LoopLab_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        spinnerRole = view.findViewById(R.id.spinnerRole);
        btnAddUser = view.findViewById(R.id.btnAddUser);
        btnCancel = view.findViewById(R.id.btnCancel);

        setupSpinner();
        setupClickListeners();
    }

    private void setupSpinner() {
        List<String> roles = new ArrayList<>();
        roles.add("student");
        roles.add("teacher");
        roles.add("admin");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnAddUser.setOnClickListener(v -> {
            if (validateInputs()) {
                addUser();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private boolean validateInputs() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void addUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        // Show loading state
        btnAddUser.setEnabled(false);
        btnAddUser.setText("Adding User...");

        // Create user in Firebase Auth
        com.google.firebase.auth.FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // Create user profile in Firestore
                    Models.UserProfile userProfile = new Models.UserProfile();
                    userProfile.uid = uid;
                    userProfile.name = name;
                    userProfile.email = email;
                    userProfile.role = role;
                    userProfile.points = 0;
                    userProfile.verified = true;
                    userProfile.isActive = true;
                    userProfile.createdAt = System.currentTimeMillis();
                    userProfile.isFirstTime = true;

                    FirebaseRefs.users().document(uid).set(userProfile)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "User added successfully!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Error saving user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnAddUser.setEnabled(true);
                                btnAddUser.setText("Add User");
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error creating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAddUser.setEnabled(true);
                    btnAddUser.setText("Add User");
                });
    }
}
