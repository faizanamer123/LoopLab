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

import java.util.ArrayList;
import java.util.List;

public class TeamMembersAdapter extends RecyclerView.Adapter<TeamMembersAdapter.TeamMemberViewHolder> {

    private List<Models.TeamMember> members = new ArrayList<>();
    private final OnTeamMemberClickListener listener;

    public interface OnTeamMemberClickListener {
        void onTeamMemberClick(Models.TeamMember member);
        void onContactClick(Models.TeamMember member);
    }

    public TeamMembersAdapter(OnTeamMemberClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Models.TeamMember> newMembers) {
        members.clear();
        if (newMembers != null) {
            members.addAll(newMembers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeamMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new TeamMemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamMemberViewHolder holder, int position) {
        holder.bind(members.get(position));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class TeamMemberViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProfilePicture;
        private final TextView tvName;
        private final TextView tvRole;
        private final TextView tvBio;
        private final MaterialButton btnContact;
        private final View indicatorOnline;

        public TeamMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvName = itemView.findViewById(R.id.tvName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvBio = itemView.findViewById(R.id.tvBio);
            btnContact = itemView.findViewById(R.id.btnContact);
            indicatorOnline = itemView.findViewById(R.id.indicatorOnline);
        }

        public void bind(Models.TeamMember member) {
            tvName.setText(member.name);
            tvRole.setText(member.role);
            tvBio.setText(member.bio);

            // Load profile picture
            if (member.photoUrl != null && !member.photoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
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

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamMemberClick(member);
                }
            });

            btnContact.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(member);
                }
            });
        }
    }
}
