package com.example.looplab.ui.home;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.looplab.R;
import com.example.looplab.ui.home.tabs.ChatFragment;
import com.example.looplab.ui.home.tabs.CoursesFragment;
import com.example.looplab.ui.home.tabs.EventsFragment;
import com.example.looplab.ui.home.tabs.HomeFragment;
import com.example.looplab.ui.home.tabs.ProfileFragment;
import com.example.looplab.ui.home.tabs.AdminDashboardFragment;
import com.example.looplab.ui.home.tabs.TeacherDashboardFragment;
import com.example.looplab.ui.home.tabs.StudentDashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private String role;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        role = getIntent().getStringExtra("role");
        bottomNavigationView = findViewById(R.id.bottomNav);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                if ("admin".equalsIgnoreCase(role)) {
                    fragment = new AdminDashboardFragment();
                } else if ("teacher".equalsIgnoreCase(role)) {
                    fragment = new TeacherDashboardFragment();
                } else {
                    fragment = new StudentDashboardFragment();
                }
            } else if (itemId == R.id.nav_courses) {
                fragment = new CoursesFragment();
            } else if (itemId == R.id.nav_events) {
                fragment = new EventsFragment();
            } else if (itemId == R.id.nav_chat) {
                fragment = new ChatFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

        public void navigateToTab(int tabIndex) {
            if (bottomNavigationView == null) return;
            if (tabIndex < 0 || tabIndex > 4) return;
            switch (tabIndex) {
                case 0:
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    break;
                case 1:
                    bottomNavigationView.setSelectedItemId(R.id.nav_courses);
                    break;
                case 2:
                    bottomNavigationView.setSelectedItemId(R.id.nav_events);
                    break;
                case 3:
                    bottomNavigationView.setSelectedItemId(R.id.nav_chat);
                    break;
                case 4:
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                    break;
            }
        }

        public void openLeaderboard() {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new com.example.looplab.ui.home.tabs.LeaderboardFragment())
                    .addToBackStack("leaderboard")
                    .commit();
        }
}


