package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.looplab.R;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

public class UserDetailDialog extends DialogFragment {

    private static final String ARG_USER = "user";
    private Models.UserProfile user;

    public static UserDetailDialog newInstance(Models.UserProfile user) {
        UserDetailDialog dialog = new UserDetailDialog();
        Bundle args = new Bundle();
        args.putString("uid", user.uid);
        args.putString("name", user.name);
        args.putString("email", user.email);
        args.putString("role", user.role);
        args.putInt("points", user.points);
        args.putBoolean("verified", user.verified);
        args.putBoolean("isActive", user.isActive);
        args.putString("photoUrl", user.photoUrl);
        args.putLong("createdAt", user.createdAt);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_LoopLab_Dialog);

        if (getArguments() != null) {
            user = new Models.UserProfile();
            user.uid = getArguments().getString("uid");
            user.name = getArguments().getString("name");
            user.email = getArguments().getString("email");
            user.role = getArguments().getString("role");
            user.points = getArguments().getInt("points");
            user.verified = getArguments().getBoolean("verified");
            user.isActive = getArguments().getBoolean("isActive");
            user.photoUrl = getArguments().getString("photoUrl");
            user.createdAt = getArguments().getLong("createdAt");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_user_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvPoints = view.findViewById(R.id.tvPoints);
        TextView tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        Chip chipRole = view.findViewById(R.id.chipRole);
        Chip chipStatus = view.findViewById(R.id.chipStatus);
        Chip chipVerified = view.findViewById(R.id.chipVerified);
        MaterialButton btnClose = view.findViewById(R.id.btnClose);

        if (user != null) {
            tvName.setText(user.name);
            tvEmail.setText(user.email);
            tvPoints.setText(user.points + " points");

            // Format creation date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            String createdDate = sdf.format(new java.util.Date(user.createdAt));
            tvCreatedAt.setText("Joined: " + createdDate);

            // Set role chip
            chipRole.setText(user.role.toUpperCase());
            switch (user.role) {
                case "admin":
                    chipRole.setChipBackgroundColorResource(R.color.error);
                    break;
                case "teacher":
                    chipRole.setChipBackgroundColorResource(R.color.warning);
                    break;
                case "student":
                    chipRole.setChipBackgroundColorResource(R.color.success);
                    break;
            }

            // Set status chip
            if (user.isActive) {
                chipStatus.setText("ACTIVE");
                chipStatus.setChipBackgroundColorResource(R.color.success);
            } else {
                chipStatus.setText("INACTIVE");
                chipStatus.setChipBackgroundColorResource(R.color.error);
            }

            // Set verified chip
            if (user.verified) {
                chipVerified.setText("VERIFIED");
                chipVerified.setChipBackgroundColorResource(R.color.success);
            } else {
                chipVerified.setText("UNVERIFIED");
                chipVerified.setChipBackgroundColorResource(R.color.warning);
            }

            // Load profile picture
            if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                Glide.with(this)
                        .load(user.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivProfilePicture);
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_profile);
            }

            btnClose.setOnClickListener(v -> dismiss());
        }
    }
}
