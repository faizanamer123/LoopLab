package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.UsersAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private CircularProgressIndicator progressIndicator;
    private UsersAdapter adapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;
    private FloatingActionButton btnAddUser;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);

        initializeViews();
        setupRecyclerView();
        setupFilters();
        setupClickListeners();
        loadUsers();
    }

    private void initializeViews() {
        rvUsers = findViewById(R.id.rvUsers);
        progressIndicator = findViewById(R.id.progressIndicator);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        btnAddUser = findViewById(R.id.btnAddUser);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new UsersAdapter(new UsersAdapter.OnUserActionListener() {
            @Override
            public void onUserClick(Models.UserProfile user) {
                showUserDetails(user);
            }

            @Override
            public void onEditUser(Models.UserProfile user) {
                editUser(user);
            }

            @Override
            public void onDeleteUser(Models.UserProfile user) {
                deleteUser(user);
            }

            @Override
            public void onToggleUserStatus(Models.UserProfile user) {
                toggleUserStatus(user);
            }
        });
        
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void setupFilters() {
        Chip chipAll = findViewById(R.id.chipAll);
        Chip chipStudents = findViewById(R.id.chipStudents);
        Chip chipTeachers = findViewById(R.id.chipTeachers);
        Chip chipAdmins = findViewById(R.id.chipAdmins);

        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            loadUsers();
        });

        chipStudents.setOnClickListener(v -> {
            currentFilter = "student";
            loadUsers();
        });

        chipTeachers.setOnClickListener(v -> {
            currentFilter = "teacher";
            loadUsers();
        });

        chipAdmins.setOnClickListener(v -> {
            currentFilter = "admin";
            loadUsers();
        });
    }

    private void setupClickListeners() {
        btnAddUser.setOnClickListener(v -> {
            // Show add user dialog
            showAddUserDialog();
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadUsers() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        Query query = FirebaseRefs.users();
        
        // Apply filter
        if (!currentFilter.equals("all")) {
            query = query.whereEqualTo("role", currentFilter);
        }
        
        query.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressIndicator.setVisibility(View.GONE);
                    List<Models.UserProfile> users = new ArrayList<>();
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            user.uid = doc.getId();
                            users.add(user);
                        }
                    }
                    
                    adapter.submitList(users);
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadSampleUsers();
                });
    }

    private void loadSampleUsers() {
        List<Models.UserProfile> sampleUsers = new ArrayList<>();
        
        // Sample students
        Models.UserProfile student1 = new Models.UserProfile();
        student1.uid = "student1";
        student1.name = "John Smith";
        student1.email = "john.smith@email.com";
        student1.role = "student";
        student1.points = 1250;
        student1.verified = true;
        student1.isActive = true;
        sampleUsers.add(student1);

        Models.UserProfile student2 = new Models.UserProfile();
        student2.uid = "student2";
        student2.name = "Sarah Johnson";
        student2.email = "sarah.johnson@email.com";
        student2.role = "student";
        student2.points = 890;
        student2.verified = true;
        student2.isActive = true;
        sampleUsers.add(student2);

        // Sample teachers
        Models.UserProfile teacher1 = new Models.UserProfile();
        teacher1.uid = "teacher1";
        teacher1.name = "Dr. Michael Chen";
        teacher1.email = "michael.chen@looplab.com";
        teacher1.role = "teacher";
        teacher1.points = 0;
        teacher1.verified = true;
        teacher1.isActive = true;
        sampleUsers.add(teacher1);

        Models.UserProfile teacher2 = new Models.UserProfile();
        teacher2.uid = "teacher2";
        teacher2.name = "Prof. Emily Davis";
        teacher2.email = "emily.davis@looplab.com";
        teacher2.role = "teacher";
        teacher2.points = 0;
        teacher2.verified = true;
        teacher2.isActive = true;
        sampleUsers.add(teacher2);

        adapter.submitList(sampleUsers);
    }

    private void filterUsers(String query) {
        // This would typically filter the current list
        // For now, we'll just show a toast
        if (!query.isEmpty()) {
            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserDetails(Models.UserProfile user) {
        UserDetailDialog dialog = UserDetailDialog.newInstance(user);
        dialog.show(getSupportFragmentManager(), "user_detail");
    }

    private void editUser(Models.UserProfile user) {
        EditUserDialog dialog = EditUserDialog.newInstance(user);
        dialog.show(getSupportFragmentManager(), "edit_user");
    }

    private void deleteUser(Models.UserProfile user) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.name + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseRefs.users().document(user.uid).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleUserStatus(Models.UserProfile user) {
        user.isActive = !user.isActive;
        FirebaseRefs.users().document(user.uid).update("isActive", user.isActive)
                .addOnSuccessListener(aVoid -> {
                    String status = user.isActive ? "activated" : "deactivated";
                    Toast.makeText(this, "User " + status + " successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddUserDialog() {
        AddUserDialog dialog = new AddUserDialog();
        dialog.show(getSupportFragmentManager(), "add_user");
    }
}
