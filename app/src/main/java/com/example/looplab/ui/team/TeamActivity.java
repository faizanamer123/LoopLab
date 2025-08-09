package com.example.looplab.ui.team;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.TeamService;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.TeamMembersAdapter;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class TeamActivity extends AppCompatActivity {

    private RecyclerView rvTeamMembers;
    private CircularProgressIndicator progressIndicator;
    private TeamMembersAdapter adapter;
    private TeamService teamService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team);

        initializeViews();
        setupRecyclerView();
        initializeServices();
        loadTeamMembers();
    }

    private void initializeViews() {
        rvTeamMembers = findViewById(R.id.rvTeamMembers);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new TeamMembersAdapter(new TeamMembersAdapter.OnTeamMemberClickListener() {
            @Override
            public void onTeamMemberClick(Models.TeamMember member) {
                // Show team member details
                showTeamMemberDetails(member);
            }

            @Override
            public void onContactClick(Models.TeamMember member) {
                // Contact team member
                contactTeamMember(member);
            }
        });
        
        rvTeamMembers.setLayoutManager(new LinearLayoutManager(this));
        rvTeamMembers.setAdapter(adapter);
    }

    private void initializeServices() {
        teamService = new TeamService();
    }

    private void loadTeamMembers() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        teamService.getAllTeamMembers(new TeamService.TeamListCallback() {
            @Override
            public void onSuccess(List<Models.TeamMember> members) {
                progressIndicator.setVisibility(View.GONE);
                adapter.submitList(members);
            }

            @Override
            public void onError(String error) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(TeamActivity.this, "Error loading team: " + error, Toast.LENGTH_SHORT).show();
                // Load sample data if network fails
                loadSampleTeamMembers();
            }
        });
    }

    private void loadSampleTeamMembers() {
        List<Models.TeamMember> sampleMembers = new ArrayList<>();
        
        Models.TeamMember member1 = new Models.TeamMember();
        member1.id = "1";
        member1.name = "Alex Johnson";
        member1.role = "Founder & CEO";
        member1.bio = "Passionate about empowering students through technology and education.";
        member1.email = "alex@looplab.com";
        member1.photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face";
        member1.isActive = true;
        sampleMembers.add(member1);

        Models.TeamMember member2 = new Models.TeamMember();
        member2.id = "2";
        member2.name = "Sarah Chen";
        member2.role = "Head of Education";
        member2.bio = "Dedicated to creating engaging learning experiences for students worldwide.";
        member2.email = "sarah@looplab.com";
        member2.photoUrl = "https://images.unsplash.com/photo-1494790108755-2616b612b786?w=150&h=150&fit=crop&crop=face";
        member2.isActive = true;
        sampleMembers.add(member2);

        Models.TeamMember member3 = new Models.TeamMember();
        member3.id = "3";
        member3.name = "Michael Rodriguez";
        member3.role = "Lead Developer";
        member3.bio = "Building innovative solutions to connect learners and educators.";
        member3.email = "michael@looplab.com";
        member3.photoUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face";
        member3.isActive = true;
        sampleMembers.add(member3);

        Models.TeamMember member4 = new Models.TeamMember();
        member4.id = "4";
        member4.name = "Emily Watson";
        member4.role = "Community Manager";
        member4.bio = "Fostering a vibrant community where students can learn, grow, and connect.";
        member4.email = "emily@looplab.com";
        member4.photoUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face";
        member4.isActive = true;
        sampleMembers.add(member4);

        adapter.submitList(sampleMembers);
    }

    private void showTeamMemberDetails(Models.TeamMember member) {
        // Show team member details dialog
        TeamMemberDetailDialog dialog = TeamMemberDetailDialog.newInstance(member);
        dialog.show(getSupportFragmentManager(), "team_member_detail");
    }

    private void contactTeamMember(Models.TeamMember member) {
        // Open email intent
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{member.email});
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Hello from LoopLab");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, "Hi " + member.name + ",\n\nI'd like to connect with you regarding LoopLab.\n\nBest regards,\n[Your Name]");
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(android.content.Intent.createChooser(intent, "Send Email"));
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
