package com.example.looplab.ui.auth.role;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.ui.home.HomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class RoleSelectionActivity extends AppCompatActivity {

    private String selectedRole = null;
    private MaterialCardView cardAdmin, cardTeacher, cardStudent;
    private MaterialButton btnContinue;
    private ImageView ivAdminCheck, ivTeacherCheck, ivStudentCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        cardAdmin = findViewById(R.id.cardAdmin);
        cardTeacher = findViewById(R.id.cardTeacher);
        cardStudent = findViewById(R.id.cardStudent);
        btnContinue = findViewById(R.id.btnContinue);

        // Get check icons from the card layouts
        ivAdminCheck = cardAdmin.findViewById(R.id.ivCheckAdmin);
        ivTeacherCheck = cardTeacher.findViewById(R.id.ivCheckTeacher);
        ivStudentCheck = cardStudent.findViewById(R.id.ivCheckStudent);
    }

    private void setupClickListeners() {
        View.OnClickListener listener = v -> {
            // Reset all selections
            resetAllSelections();
            
            // Set new selection
            if (v.getId() == R.id.cardAdmin) {
                selectedRole = "admin";
                selectCard(cardAdmin, ivAdminCheck);
            } else if (v.getId() == R.id.cardTeacher) {
                selectedRole = "teacher";
                selectCard(cardTeacher, ivTeacherCheck);
            } else if (v.getId() == R.id.cardStudent) {
                selectedRole = "student";
                selectCard(cardStudent, ivStudentCheck);
            }
            
            // Enable continue button
            btnContinue.setEnabled(true);
            animateButton();
        };

        cardAdmin.setOnClickListener(listener);
        cardTeacher.setOnClickListener(listener);
        cardStudent.setOnClickListener(listener);

        btnContinue.setOnClickListener(v -> {
            if (selectedRole != null) {
                // Save role to Firebase
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
                if (currentUserId != null) {
                    // Update user profile with selected role
                    FirebaseRefs.users().document(currentUserId).update("role", selectedRole, "isFirstTime", false)
                            .addOnSuccessListener(aVoid -> {
                                // Navigate to home with role
                                Intent intent = new Intent(RoleSelectionActivity.this, HomeActivity.class);
                                intent.putExtra("role", selectedRole);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error saving role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resetAllSelections() {
        // Reset all cards to unselected state
        cardAdmin.setSelected(false);
        cardTeacher.setSelected(false);
        cardStudent.setSelected(false);
        
        // Hide all check icons
        if (ivAdminCheck != null) ivAdminCheck.setVisibility(View.GONE);
        if (ivTeacherCheck != null) ivTeacherCheck.setVisibility(View.GONE);
        if (ivStudentCheck != null) ivStudentCheck.setVisibility(View.GONE);
    }

    private void selectCard(MaterialCardView card, ImageView checkIcon) {
        // Set card as selected
        card.setSelected(true);
        
        // Show check icon with animation
        if (checkIcon != null) {
            checkIcon.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            checkIcon.startAnimation(fadeIn);
        }
        
        // Add selection animation
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        card.startAnimation(scaleAnimation);
    }

    private void animateButton() {
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        btnContinue.startAnimation(scaleAnimation);
    }
}


