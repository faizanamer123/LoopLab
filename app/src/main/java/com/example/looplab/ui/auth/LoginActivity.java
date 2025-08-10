package com.example.looplab.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.ui.auth.role.RoleSelectionActivity;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private CallbackManager callbackManager;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        auth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        boolean hasWebClientId = webClientId != null && !"REPLACE_ME".equals(webClientId) && webClientId.trim().length() > 0;
        if (hasWebClientId) {
            gsoBuilder.requestIdToken(webClientId);
        }
        googleClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etEmail = findViewById(R.id.etEmail);
                EditText etPassword = findViewById(R.id.etPassword);
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(result -> checkUserRoleAndProceed())
                        .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        // Setup Google Sign In Button
        View googleBtn = findViewById(R.id.btnGoogle);
        if (!hasWebClientId) {
            googleBtn.setEnabled(false);
            googleBtn.setAlpha(0.6f);
            googleBtn.setOnClickListener(v -> Toast.makeText(this, "Google Sign-In not configured", Toast.LENGTH_SHORT).show());
        } else {
            googleBtn.setOnClickListener(v -> {
                Intent signInIntent = googleClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        // Setup Facebook Sign In Button
        findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            // Handle Facebook login - you can implement Facebook SDK here
            // For now, this is a placeholder
            Toast.makeText(this, "Facebook login coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.tvCreateAccount).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void checkUserRoleAndProceed() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Check if user already has a role in Firestore
            com.example.looplab.data.FirebaseRefs.users().document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if (role != null && !role.isEmpty()) {
                                // User has a role, go directly to home
                                Intent intent = new Intent(LoginActivity.this, com.example.looplab.ui.home.HomeActivity.class);
                                intent.putExtra("role", role);
                                startActivity(intent);
                            } else {
                                // User exists but no role, go to role selection
                                proceedToRoleSelection();
                            }
                        } else {
                            // User exists in auth but not in Firestore, go to role selection
                            proceedToRoleSelection();
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Error checking user data, go to role selection as fallback
                        proceedToRoleSelection();
                    });
        } else {
            // No user, go to role selection
            proceedToRoleSelection();
        }
    }

    private void proceedToRoleSelection() {
        startActivity(new Intent(LoginActivity.this, RoleSelectionActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK if needed
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google sign-in failed: No ID token", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, t -> {
            if (t.isSuccessful()) {
                checkUserRoleAndProceed();
            } else {
                Toast.makeText(this, "Auth failed: " + (t.getException() != null ? t.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
        });
    }
}