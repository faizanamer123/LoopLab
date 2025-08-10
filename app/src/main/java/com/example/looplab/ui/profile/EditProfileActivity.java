package com.example.looplab.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private TextInputEditText etName, etBio;
    private TextInputLayout tilName, tilBio;
    private MaterialButton btnSave, btnCancel, btnChangePhoto;
    private Uri selectedImageUri;
    private Models.UserProfile currentUserProfile;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageRef = storage.getReference();
    private boolean isImageChanged = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        isImageChanged = true;
                        ivProfilePicture.setImageURI(selectedImageUri);
                        animateImageChange();
                        showImagePreview();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        setupAnimations();
        loadCurrentUserData();
        setupClickListeners();
        setupTextWatchers();
        setupBackPressedCallback();
    }

    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Add exit animation
                Animation slideOut = AnimationUtils.loadAnimation(EditProfileActivity.this, android.R.anim.slide_out_right);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        finish();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                findViewById(android.R.id.content).startAnimation(slideOut);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etName = findViewById(R.id.etName);
        etBio = findViewById(R.id.etBio);
        tilName = findViewById(R.id.tilName);
        tilBio = findViewById(R.id.tilBio);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
    }

    private void setupAnimations() {
        // Animate views on startup
        Animation slideInLeft = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        Animation slideInRight = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        ivProfilePicture.startAnimation(fadeIn);
        btnChangePhoto.startAnimation(fadeIn);
        tilName.startAnimation(slideInLeft);
        tilBio.startAnimation(slideInLeft);
    }

    private void setupTextWatchers() {
        // Enhanced text validation with real-time feedback
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateName();
            }
        });

        etBio.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                animateInputFocus(tilBio);
            }
        });
    }

    private boolean validateName() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            tilName.setError("Name is required");
            tilName.setErrorEnabled(true);
            animateError(tilName);
            return false;
        } else if (name.length() < 2) {
            tilName.setError("Name must be at least 2 characters");
            tilName.setErrorEnabled(true);
            animateError(tilName);
            return false;
        } else {
            tilName.setError(null);
            tilName.setErrorEnabled(false);
            animateSuccess(tilName);
            return true;
        }
    }

    private void animateInputFocus(TextInputLayout layout) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.02f, 1.0f, 1.02f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(200);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        layout.startAnimation(scaleAnimation);
    }

    private void animateError(TextInputLayout layout) {
        Animation shake = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        layout.startAnimation(shake);
    }

    private void animateSuccess(TextInputLayout layout) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.05f, 1.0f, 1.05f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(150);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        layout.startAnimation(scaleAnimation);
    }

    private void animateImageChange() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(300);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        ivProfilePicture.startAnimation(scaleAnimation);
    }

    private void showImagePreview() {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                "Profile picture updated!", Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        snackbar.show();
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEnhancedError("User not authenticated");
            finish();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Load user profile from Firestore
        String uid = currentUser.getUid();
        FirebaseRefs.users().document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoadingState(false);
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
                    populateUIWithAnimation();
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    showEnhancedError("Failed to load profile: " + e.getMessage());
                });
    }

    private void populateUIWithAnimation() {
        // Animate text appearing
        if (currentUserProfile.name != null) {
            etName.setText(currentUserProfile.name);
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            etName.startAnimation(fadeIn);
        }

        if (currentUserProfile.bio != null) {
            etBio.setText(currentUserProfile.bio);
            Animation slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            etBio.startAnimation(slideIn);
        }

        // Load profile picture if exists
        if (currentUserProfile.photoUrl != null && !currentUserProfile.photoUrl.isEmpty()) {
            // You can use Glide or Picasso here to load the image
            // For now, we'll just show a placeholder with animation
            Animation rotateIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            ivProfilePicture.startAnimation(rotateIn);
            ivProfilePicture.setImageResource(R.drawable.ic_profile);
        }
    }

    private void setLoadingState(boolean loading) {
        btnSave.setEnabled(!loading);
        btnChangePhoto.setEnabled(!loading);

        if (loading) {
            btnSave.setText("Loading...");
            // Add subtle pulsing animation to save button
            Animation pulseAnimation = new ScaleAnimation(
                    1.0f, 1.05f, 1.0f, 1.05f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulseAnimation.setDuration(1000);
            pulseAnimation.setRepeatCount(Animation.INFINITE);
            pulseAnimation.setRepeatMode(Animation.REVERSE);
            btnSave.startAnimation(pulseAnimation);
        } else {
            btnSave.setText("Save Changes");
            btnSave.clearAnimation();
        }
    }

    private void setupClickListeners() {
        // Enhanced click listeners with animations
        btnChangePhoto.setOnClickListener(v -> {
            animateButtonClick(btnChangePhoto);
            openImagePicker();
        });

        btnSave.setOnClickListener(v -> {
            animateButtonClick(btnSave);
            saveProfile();
        });

        btnCancel.setOnClickListener(v -> {
            animateButtonClick(btnCancel);
            handleBackPress();
        });

        // Add back button functionality
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            animateButtonClick(v);
            handleBackPress();
        });
    }

    private void animateButtonClick(View button) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(100);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        button.startAnimation(scaleAnimation);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        if (!validateName()) {
            return;
        }

        String name = etName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        // Enhanced save button state
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Add progress animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        btnSave.startAnimation(rotateAnimation);

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
                            resetSaveButton();
                            showEnhancedError("Failed to update profile: " + task.getException().getMessage());
                        }
                    });
        }
    }

    private void resetSaveButton() {
        btnSave.setEnabled(true);
        btnSave.setText("Save Changes");
        btnSave.clearAnimation();
    }

    private void updateFirestoreProfile(String name, String bio) {
        if (currentUserProfile == null) return;

        // Update profile data
        currentUserProfile.name = name;
        currentUserProfile.bio = bio;
        currentUserProfile.lastActive = System.currentTimeMillis();

        // If there's a new image, upload it first
        if (selectedImageUri != null && isImageChanged) {
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
                                resetSaveButton();
                                showEnhancedError("Failed to get image URL: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    resetSaveButton();
                    showEnhancedError("Failed to upload image: " + e.getMessage());
                });
    }

    private void saveProfileToFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseRefs.users().document(uid).set(currentUserProfile)
                .addOnSuccessListener(aVoid -> {
                    resetSaveButton();
                    showSuccessMessage();

                    // Add success animation
                    Animation successAnimation = new ScaleAnimation(
                            1.0f, 1.1f, 1.0f, 1.1f,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    successAnimation.setDuration(300);
                    successAnimation.setRepeatCount(1);
                    successAnimation.setRepeatMode(Animation.REVERSE);
                    btnSave.startAnimation(successAnimation);

                    // Delay finish to show success animation
                    btnSave.postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    resetSaveButton();
                    showEnhancedError("Failed to save profile: " + e.getMessage());
                });
    }

    private void handleBackPress() {
        // Add slide out animation before finishing
        Animation slideOut = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        findViewById(android.R.id.content).startAnimation(slideOut);
    }

    private void showSuccessMessage() {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                "Profile updated successfully!", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        snackbar.show();
    }

    private void showEnhancedError(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        snackbar.setAction("OK", v -> snackbar.dismiss());
        snackbar.show();
    }
}