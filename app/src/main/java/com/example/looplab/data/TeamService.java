package com.example.looplab.data;

import android.util.Log;

import com.example.looplab.data.model.Models;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class TeamService {
    private static final String TAG = "TeamService";
    
    public interface TeamCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface TeamListCallback {
        void onSuccess(List<Models.TeamMember> members);
        void onError(String error);
    }
    
    // Add team member
    public void addTeamMember(Models.TeamMember member, TeamCallback callback) {
        member.id = FirebaseRefs.team().document().getId();
        
        FirebaseRefs.team().document(member.id).set(member.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Team member added: " + member.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding team member", e);
                    callback.onError("Failed to add team member: " + e.getMessage());
                });
    }
    
    // Update team member
    public void updateTeamMember(Models.TeamMember member, TeamCallback callback) {
        FirebaseRefs.team().document(member.id).update(member.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Team member updated: " + member.id);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating team member", e);
                    callback.onError("Failed to update team member: " + e.getMessage());
                });
    }
    
    // Delete team member
    public void deleteTeamMember(String memberId, TeamCallback callback) {
        FirebaseRefs.team().document(memberId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Team member deleted: " + memberId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting team member", e);
                    callback.onError("Failed to delete team member: " + e.getMessage());
                });
    }
    
    // Get all team members
    public void getAllTeamMembers(TeamListCallback callback) {
        FirebaseRefs.team().whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.TeamMember> members = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.TeamMember member = doc.toObject(Models.TeamMember.class);
                        if (member != null) {
                            member.id = doc.getId();
                            members.add(member);
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting team members", e);
                    callback.onError("Failed to get team members: " + e.getMessage());
                });
    }
    
    // Get team member by ID
    public void getTeamMember(String memberId, TeamListCallback callback) {
        FirebaseRefs.team().document(memberId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Models.TeamMember member = documentSnapshot.toObject(Models.TeamMember.class);
                        if (member != null) {
                            member.id = documentSnapshot.getId();
                            List<Models.TeamMember> members = new ArrayList<>();
                            members.add(member);
                            callback.onSuccess(members);
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting team member", e);
                    callback.onError("Failed to get team member: " + e.getMessage());
                });
    }
    
    // Initialize default team members
    public void initializeDefaultTeam() {
        List<Models.TeamMember> defaultMembers = new ArrayList<>();
        
        // Founder/CEO
        Models.TeamMember founder = new Models.TeamMember();
        founder.id = "founder";
        founder.name = "LoopLab Founder";
        founder.role = "Founder & CEO";
        founder.bio = "Passionate about empowering students through technology and education.";
        founder.email = "founder@looplab.com";
        founder.isActive = true;
        founder.createdAt = System.currentTimeMillis();
        defaultMembers.add(founder);
        
        // CTO
        Models.TeamMember cto = new Models.TeamMember();
        cto.id = "cto";
        cto.name = "Tech Lead";
        cto.role = "Chief Technology Officer";
        cto.bio = "Leading the technical vision and development of LoopLab platform.";
        cto.email = "tech@looplab.com";
        cto.isActive = true;
        cto.createdAt = System.currentTimeMillis();
        defaultMembers.add(cto);
        
        // Head of Education
        Models.TeamMember educationLead = new Models.TeamMember();
        educationLead.id = "education_lead";
        educationLead.name = "Education Lead";
        educationLead.role = "Head of Education";
        educationLead.bio = "Creating engaging learning experiences and curriculum for students.";
        educationLead.email = "education@looplab.com";
        educationLead.isActive = true;
        educationLead.createdAt = System.currentTimeMillis();
        defaultMembers.add(educationLead);
        
        // Community Manager
        Models.TeamMember communityManager = new Models.TeamMember();
        communityManager.id = "community_manager";
        communityManager.name = "Community Manager";
        communityManager.role = "Community & Events Lead";
        communityManager.bio = "Building and nurturing our vibrant student community.";
        communityManager.email = "community@looplab.com";
        communityManager.isActive = true;
        communityManager.createdAt = System.currentTimeMillis();
        defaultMembers.add(communityManager);
        
        // Add all default members to Firestore
        for (Models.TeamMember member : defaultMembers) {
            FirebaseRefs.team().document(member.id).set(member.toMap())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Default team member added: " + member.name))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding default team member: " + member.name, e));
        }
    }
    
    // Get team member by role
    public void getTeamMembersByRole(String role, TeamListCallback callback) {
        FirebaseRefs.team().whereEqualTo("role", role)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.TeamMember> members = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.TeamMember member = doc.toObject(Models.TeamMember.class);
                        if (member != null) {
                            member.id = doc.getId();
                            members.add(member);
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting team members by role", e);
                    callback.onError("Failed to get team members by role: " + e.getMessage());
                });
    }
    
    // Search team members
    public void searchTeamMembers(String query, TeamListCallback callback) {
        FirebaseRefs.team().whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Models.TeamMember> members = new ArrayList<>();
                    String lowerQuery = query.toLowerCase();
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.TeamMember member = doc.toObject(Models.TeamMember.class);
                        if (member != null) {
                            member.id = doc.getId();
                            
                            // Check if member matches search query
                            if (member.name.toLowerCase().contains(lowerQuery) ||
                                member.role.toLowerCase().contains(lowerQuery) ||
                                (member.bio != null && member.bio.toLowerCase().contains(lowerQuery))) {
                                members.add(member);
                            }
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching team members", e);
                    callback.onError("Failed to search team members: " + e.getMessage());
                });
    }
    
    // Get team statistics
    public void getTeamStats(TeamCallback callback) {
        FirebaseRefs.team().whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalMembers = querySnapshot.size();
                    int founders = 0;
                    int technical = 0;
                    int education = 0;
                    int community = 0;
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.TeamMember member = doc.toObject(Models.TeamMember.class);
                        if (member != null) {
                            String role = member.role.toLowerCase();
                            if (role.contains("founder") || role.contains("ceo")) {
                                founders++;
                            } else if (role.contains("tech") || role.contains("cto") || role.contains("developer")) {
                                technical++;
                            } else if (role.contains("education") || role.contains("teacher") || role.contains("instructor")) {
                                education++;
                            } else if (role.contains("community") || role.contains("event") || role.contains("manager")) {
                                community++;
                            }
                        }
                    }
                    
                    Log.d(TAG, String.format("Team stats - Total: %d, Founders: %d, Tech: %d, Education: %d, Community: %d",
                            totalMembers, founders, technical, education, community));
                    
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting team stats", e);
                    callback.onError("Failed to get team stats: " + e.getMessage());
                });
    }
} 