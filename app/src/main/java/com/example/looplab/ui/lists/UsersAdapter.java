package com.example.looplab.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.looplab.R;
import com.example.looplab.data.model.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<Models.UserProfile> users = new ArrayList<>();
    private final OnUserActionListener listener;

    // For multi-selection
    private final Set<String> selectedUserIds = new HashSet<>();

    public interface OnUserActionListener {
        void onUserClick(Models.UserProfile user);
        void onEditUser(Models.UserProfile user);
        void onDeleteUser(Models.UserProfile user);
        void onToggleUserStatus(Models.UserProfile user);
    }

    public UsersAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Models.UserProfile> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(String userId) {
        if (selectedUserIds.contains(userId)) {
            selectedUserIds.remove(userId);
        } else {
            selectedUserIds.add(userId);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedUserIds.clear();
        notifyDataSetChanged();
    }

    public List<Models.UserProfile> getSelectedUsers() {
        List<Models.UserProfile> selected = new ArrayList<>();
        for (Models.UserProfile user : users) {
            if (selectedUserIds.contains(user.uid)) {
                selected.add(user);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProfilePicture;
        private final TextView tvName;
        private final TextView tvEmail;
        private final Chip chipRole;
        private final TextView tvPoints;
        private final Chip chipStatus;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;
        private final MaterialButton btnToggleStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            chipRole = itemView.findViewById(R.id.chipRole);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
        }

        public void bind(Models.UserProfile user) {
            tvName.setText(user.name);
            tvEmail.setText(user.email);
            tvPoints.setText(user.points + " pts");

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

            // Load profile picture
            if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivProfilePicture);
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_profile);
            }

            // Highlight if selected
            itemView.setBackgroundColor(
                    selectedUserIds.contains(user.uid)
                            ? itemView.getResources().getColor(R.color.selection_highlight)
                            : itemView.getResources().getColor(android.R.color.transparent)
            );

            // Selection toggle on long press
            itemView.setOnLongClickListener(v -> {
                toggleSelection(user.uid);
                return true;
            });

            // Normal click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditUser(user);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteUser(user);
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) listener.onToggleUserStatus(user);
            });

            // Update toggle button text
            btnToggleStatus.setText(user.isActive ? "Deactivate" : "Activate");
        }
    }
}
