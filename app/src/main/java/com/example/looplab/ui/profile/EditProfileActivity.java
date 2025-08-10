package com.example.looplab.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private TextInputEditText etName, etBio;
    private MaterialButton btnSave, btnCancel, btnChangePhoto;
    private Uri selectedImageUri;
    private Models.UserProfile currentUserProfile;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageRef = storage.getReference();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    ivProfilePicture.setImageURI(selectedImageUri);
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        loadCurrentUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etName = findViewById(R.id.etName);
        etBio = findViewById(R.id.etBio);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load user profile from Firestore
        String uid = currentUser.getUid();
        FirebaseRefs.users().document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                currentUserProfile = documentSnapshot.toObject(Models.UserProfile.class);
                if (currentUserProfile == null) {
                    // Create new profile if doesn't exist
                    currentUserProfile = new Models.UserProfile();
                    currentUserProfile.uid = uid;
                    currentUserProfile.name = currentUser.getDisplayName();
                    currentUserProfile.email = currentUser.getEmail();
                    currentUserProfile.role = "student";
                    currentUserProfile.points = 0;
                    currentUserProfile.createdAt = System.currentTimeMillis();
                    currentUserProfile.lastActive = System.currentTimeMillis();
                    currentUserProfile.isActive = true;
                    currentUserProfile.isFirstTime = true;
                }

                // Populate UI with current data
                etName.setText(currentUserProfile.name);
                if (currentUserProfile.bio != null) {
                    etBio.setText(currentUserProfile.bio);
                }

                // Load profile picture if exists
                if (currentUserProfile.photoUrl != null && !currentUserProfile.photoUrl.isEmpty()) {
                    // You can use Glide or Picasso here to load the image
                    // For now, we'll just show a placeholder
                    ivProfilePicture.setImageResource(R.drawable.ic_profile);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        
        btnSave.setOnClickListener(v -> saveProfile());
        
        btnCancel.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Update Firebase Auth display name
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update Firestore profile
                        updateFirestoreProfile(name, bio);
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this, "Failed to update profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void updateFirestoreProfile(String name, String bio) {
        if (currentUserProfile == null) return;

        // Update profile data
        currentUserProfile.name = name;
        currentUserProfile.bio = bio;
        currentUserProfile.lastActive = System.currentTimeMillis();

        // If there's a new image, upload it first
        if (selectedImageUri != null) {
            uploadProfileImage();
        } else {
            // Save profile without image
            saveProfileToFirestore();
        }
    }

    private void uploadProfileImage() {
        if (selectedImageUri == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference imageRef = storageRef.child("profile_images/" + uid + ".jpg");

        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                imageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        currentUserProfile.photoUrl = uri.toString();
                        saveProfileToFirestore();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void saveProfileToFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        FirebaseRefs.users().document(uid).set(currentUserProfile)
            .addOnSuccessListener(aVoid -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
