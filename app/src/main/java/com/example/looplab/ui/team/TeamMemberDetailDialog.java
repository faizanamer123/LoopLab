package com.example.looplab.ui.team;

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

public class TeamMemberDetailDialog extends DialogFragment {

    private static final String ARG_MEMBER = "member";
    private Models.TeamMember member;

    public static TeamMemberDetailDialog newInstance(Models.TeamMember member) {
        TeamMemberDetailDialog dialog = new TeamMemberDetailDialog();
        Bundle args = new Bundle();
        // For simplicity, we'll pass member data as individual strings
        args.putString("id", member.id);
        args.putString("name", member.name);
        args.putString("role", member.role);
        args.putString("bio", member.bio);
        args.putString("email", member.email);
        args.putString("photoUrl", member.photoUrl);
        args.putBoolean("isActive", member.isActive);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_LoopLab_Dialog);
        
        if (getArguments() != null) {
            member = new Models.TeamMember();
            member.id = getArguments().getString("id");
            member.name = getArguments().getString("name");
            member.role = getArguments().getString("role");
            member.bio = getArguments().getString("bio");
            member.email = getArguments().getString("email");
            member.photoUrl = getArguments().getString("photoUrl");
            member.isActive = getArguments().getBoolean("isActive");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_team_member_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvRole = view.findViewById(R.id.tvRole);
        TextView tvBio = view.findViewById(R.id.tvBio);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        MaterialButton btnContact = view.findViewById(R.id.btnContact);
        MaterialButton btnClose = view.findViewById(R.id.btnClose);
        View indicatorOnline = view.findViewById(R.id.indicatorOnline);

        if (member != null) {
            tvName.setText(member.name);
            tvRole.setText(member.role);
            tvBio.setText(member.bio);
            tvEmail.setText(member.email);

            // Load profile picture
            if (member.photoUrl != null && !member.photoUrl.isEmpty()) {
                Glide.with(this)
                        .load(member.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivProfilePicture);
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_profile);
            }

            // Show online indicator
            indicatorOnline.setVisibility(member.isActive ? View.VISIBLE : View.GONE);

            btnContact.setOnClickListener(v -> {
                // Contact team member via email
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("message/rfc822");
                    intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{member.email});
                    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Hello from LoopLab");
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi " + member.name + ",\n\nI'd like to connect with you regarding LoopLab.\n\nBest regards,\n[Your Name]");
                    
                    if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        startActivity(android.content.Intent.createChooser(intent, "Send Email"));
                    } else {
                        android.widget.Toast.makeText(requireContext(), "No email app found", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.widget.Toast.makeText(requireContext(), "Error opening email: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            btnClose.setOnClickListener(v -> dismiss());
        }
    }
}
