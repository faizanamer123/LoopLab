package com.example.looplab.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.ui.auth.LoginActivity;
import com.example.looplab.ui.auth.role.RoleSelectionActivity;
import com.example.looplab.ui.home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Check if user is already logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentUser != null) {
                // User is logged in, check if they have a role
                FirebaseRefs.users().document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String role = documentSnapshot.getString("role");
                                if (role != null && !role.isEmpty()) {
                                    // User has a role, go to home
                                    Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                                    intent.putExtra("role", role);
                                    startActivity(intent);
                                } else {
                                    // User exists but no role, go to role selection
                                    startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                                }
                            } else {
                                // User exists in auth but not in Firestore, go to role selection
                                startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                            }
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // Error checking user data, go to login
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                        });
            } else {
                // No user logged in, go to login
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 1500); // Increased delay for better UX
    }
}


