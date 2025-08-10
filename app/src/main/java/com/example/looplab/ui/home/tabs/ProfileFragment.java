package com.example.looplab.ui.home.tabs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.looplab.R;
import com.example.looplab.ui.settings.SettingsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvEnrolledCount, tvCompletedCount, tvPointsCount;
    private CardView cardEditProfile, cardSettings, cardLeaderboard;
    private MaterialButton btnToggleTheme;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        
        initializeViews(root);
        loadUserData();
        setupClickListeners();
        
        return root;
    }

    private void initializeViews(View root) {
        ivProfilePicture = root.findViewById(R.id.ivProfilePicture);
        tvUserName = root.findViewById(R.id.tvUserName);
        tvUserEmail = root.findViewById(R.id.tvUserEmail);
        tvUserRole = root.findViewById(R.id.tvUserRole);
        tvEnrolledCount = root.findViewById(R.id.tvEnrolledCount);
        tvCompletedCount = root.findViewById(R.id.tvCompletedCount);
        tvPointsCount = root.findViewById(R.id.tvPointsCount);
        cardEditProfile = root.findViewById(R.id.cardEditProfile);
        cardSettings = root.findViewById(R.id.cardSettings);
        btnToggleTheme = root.findViewById(R.id.btnToggleTheme);
        cardLeaderboard = root.findViewById(R.id.cardLeaderboard);
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        tvUserName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        tvUserEmail.setText(currentUser.getEmail());

        String uid = currentUser.getUid();
        com.example.looplab.data.FirebaseRefs.users().document(uid).get()
                .addOnSuccessListener(doc -> {
                    com.example.looplab.data.model.Models.UserProfile user = doc.toObject(com.example.looplab.data.model.Models.UserProfile.class);
                    if (user != null) {
                        tvUserRole.setText(user.role != null ? capitalize(user.role) : "Student");
                        tvPointsCount.setText(String.valueOf(user.points));
                    }
                });

        // enrolled count
        com.example.looplab.data.FirebaseRefs.enrollments().whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snap -> tvEnrolledCount.setText(String.valueOf(snap.size())));

        // completed count
        com.example.looplab.data.FirebaseRefs.enrollments().whereEqualTo("userId", uid)
                .whereEqualTo("progress", 100).get()
                .addOnSuccessListener(snap -> tvCompletedCount.setText(String.valueOf(snap.size())));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void setupClickListeners() {
        // Theme toggle
        btnToggleTheme.setOnClickListener(v -> {
            int current = AppCompatDelegate.getDefaultNightMode();
            int next = (current == AppCompatDelegate.MODE_NIGHT_YES) ? 
                AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            AppCompatDelegate.setDefaultNightMode(next);
            
            Toast.makeText(getContext(), "Theme changed", Toast.LENGTH_SHORT).show();
        });

        // Edit Profile
        cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.looplab.ui.profile.EditProfileActivity.class);
            startActivityForResult(intent, 1001);
        });

        // Settings
        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        // Leaderboard
        cardLeaderboard.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((com.example.looplab.ui.home.HomeActivity) getActivity()).openLeaderboard();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            // Profile was updated, reload user data
            loadUserData();
        }
    }
}


