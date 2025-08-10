package com.example.looplab.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.UsersAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public abstract class CompleteUserManagementActivity extends AppCompatActivity implements UsersAdapter.OnUserActionListener {

    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;
    private RecyclerView rvUsers;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddUser;
    private TextView tvUserCount;
    private MaterialButton btnExportUsers, btnBulkActions;

    private UsersAdapter adapter;
    private List<Models.UserProfile> allUsers = new ArrayList<>();
    private List<Models.UserProfile> filteredUsers = new ArrayList<>();
    private String currentFilter = "all";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_complete_user_management);

        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupSearchListener();
        setupFilterListeners();
        setupRecyclerView();
        loadUsers();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        rvUsers = findViewById(R.id.rvUsers);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyState = findViewById(R.id.emptyState);
        fabAddUser = findViewById(R.id.fabAddUser);
        tvUserCount = findViewById(R.id.tvUserCount);
        btnExportUsers = findViewById(R.id.btnExportUsers);
        btnBulkActions = findViewById(R.id.btnBulkActions);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        fabAddUser.setOnClickListener(v -> showAddUserDialog());
        btnExportUsers.setOnClickListener(v -> exportUsers());
        btnBulkActions.setOnClickListener(v -> showBulkActionsDialog());
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterUsers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterListeners() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) {
                currentFilter = chip.getTag().toString();
                filterUsers();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new UsersAdapter(this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        progressIndicator.setVisibility(View.VISIBLE);
        
        FirebaseRefs.users()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    progressIndicator.setVisibility(View.GONE);
                    
                    if (e != null || snapshot == null) {
                        showEmptyState();
                        return;
                    }
                    
                    allUsers.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Models.UserProfile user = doc.toObject(Models.UserProfile.class);
                        if (user != null) {
                            user.uid = doc.getId();
                            allUsers.add(user);
                        }
                    }
                    
                    filterUsers();
                });
    }

    private void filterUsers() {
        filteredUsers.clear();
        
        for (Models.UserProfile user : allUsers) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                user.name.toLowerCase().contains(searchQuery) ||
                user.email.toLowerCase().contains(searchQuery);
            
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "admin":
                    matchesFilter = "admin".equals(user.role);
                    break;
                case "teacher":
                    matchesFilter = "teacher".equals(user.role);
                    break;
                case "student":
                    matchesFilter = "student".equals(user.role);
                    break;
                case "active":
                    matchesFilter = user.isActive;
                    break;
                case "suspended":
                    matchesFilter = !user.isActive;
                    break;
            }
            
            if (matchesSearch && matchesFilter) {
                filteredUsers.add(user);
            }
        }
        
        updateDisplay();
    }

    private void updateDisplay() {
        if (filteredUsers.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            adapter.submitList(new ArrayList<>(filteredUsers));
        }
        
        tvUserCount.setText(filteredUsers.size() + " users");
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        rvUsers.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        rvUsers.setVisibility(View.VISIBLE);
    }

    private void showAddUserDialog() {
        Intent intent = new Intent(this, AddEditUserActivity.class);
        intent.putExtra("isEdit", false);
        startActivity(intent);
    }

    private void exportUsers() {
        // Export users to CSV or email
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Email,Role,Active,Created At\n");
        
        for (Models.UserProfile user : filteredUsers) {
            csv.append(user.name).append(",")
               .append(user.email).append(",")
               .append(user.role).append(",")
               .append(user.isActive).append(",")
               .append(new java.util.Date(user.createdAt)).append("\n");
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "LoopLab Users Export");
        startActivity(Intent.createChooser(shareIntent, "Export Users"));
    }

    private void showBulkActionsDialog() {
        String[] actions = {"Activate Selected", "Suspend Selected", "Delete Selected", "Change Role"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Bulk Actions")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            performBulkAction("activate");
                            break;
                        case 1:
                            performBulkAction("suspend");
                            break;
                        case 2:
                            confirmBulkDelete();
                            break;
                        case 3:
                            showBulkRoleChangeDialog();
                            break;
                    }
                })
                .show();
    }

    private void performBulkAction(String action) {
        List<Models.UserProfile> selectedUsers = new ArrayList<>(); // placeholder until selection support added
        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        for (Models.UserProfile user : selectedUsers) {
            switch (action) {
                case "activate":
                    FirebaseRefs.users().document(user.uid).update("isActive", true);
                    break;
                case "suspend":
                    FirebaseRefs.users().document(user.uid).update("isActive", false);
                    break;
            }
        }
        
        Toast.makeText(this, "Bulk action completed for " + selectedUsers.size() + " users", 
                Toast.LENGTH_SHORT).show();
        // no selection model implemented
    }

    private void confirmBulkDelete() {
        List<Models.UserProfile> selectedUsers = new ArrayList<>();
        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Users")
                .setMessage("Are you sure you want to delete " + selectedUsers.size() + " users? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (Models.UserProfile user : selectedUsers) {
                        FirebaseRefs.users().document(user.uid).delete();
                    }
                    Toast.makeText(this, "Deleted " + selectedUsers.size() + " users", 
                            Toast.LENGTH_SHORT).show();
                    // no selection model implemented
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBulkRoleChangeDialog() {
        List<Models.UserProfile> selectedUsers = new ArrayList<>();
        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] roles = {"student", "teacher", "admin"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Role for " + selectedUsers.size() + " users")
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    for (Models.UserProfile user : selectedUsers) {
                        FirebaseRefs.users().document(user.uid).update("role", newRole);
                    }
                    Toast.makeText(this, "Changed role to " + newRole + " for " + selectedUsers.size() + " users", 
                            Toast.LENGTH_SHORT).show();
                    // no selection model implemented
                })
                .show();
    }

    // UsersAdapter.OnUserActionListener implementation
    @Override
    public void onUserClick(Models.UserProfile user) {
        UserDetailDialog dialog = UserDetailDialog.newInstance(user);
        dialog.show(getSupportFragmentManager(), "user_detail");
    }

    @Override
    public void onEditUser(Models.UserProfile user) {
        Intent intent = new Intent(this, AddEditUserActivity.class);
        intent.putExtra("isEdit", true);
        intent.putExtra("userId", user.uid);
        startActivity(intent);
    }

    public void onToggleUserStatus(Models.UserProfile user) {
        String action = user.isActive ? "suspend" : "activate";
        String message = user.isActive ? "suspend" : "activate";
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Action")
                .setMessage("Are you sure you want to " + message + " " + user.name + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseRefs.users().document(user.uid)
                            .update("isActive", !user.isActive)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "User " + message + "d successfully", 
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to " + message + " user: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeleteUser(Models.UserProfile user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.name + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseRefs.users().document(user.uid).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete user: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
