package com.example.looplab.ui.home.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.example.looplab.R;
import com.example.looplab.data.GamificationService;
import com.example.looplab.data.model.Models;
import com.example.looplab.ui.lists.LeaderboardAdapter;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaderboardFragment extends Fragment {

    private LeaderboardAdapter adapter;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout emptyState;
    private TextView tvLeaderboardTitle;
    private Toolbar toolbar;
    private SimpleDateFormat dateFormat;
    private androidx.recyclerview.widget.RecyclerView rvLeaderboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        
        initializeViews(root);
        setupToolbar();
        setupRecyclerView();
        loadLeaderboard();
        
        return root;
    }

    private void initializeViews(View root) {
        progressIndicator = root.findViewById(R.id.progressIndicator);
        emptyState = root.findViewById(R.id.emptyState);
        tvLeaderboardTitle = root.findViewById(R.id.tvLeaderboardTitle);
        toolbar = root.findViewById(R.id.toolbar);
        rvLeaderboard = root.findViewById(R.id.rvLeaderboard);
        
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("Leaderboard");
            toolbar.setSubtitle(dateFormat.format(new Date()));
        }
    }

    private void setupRecyclerView() {
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter();
        rvLeaderboard.setAdapter(adapter);
    }

    private void loadLeaderboard() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }

        GamificationService gamificationService = new GamificationService();
        gamificationService.getLeaderboard(new GamificationService.LeaderboardCallback() {
            @Override
            public void onSuccess(List<Models.LeaderboardEntry> entries) {
                if (progressIndicator != null) {
                    progressIndicator.setVisibility(View.GONE);
                }
                
                if (entries.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    List<LeaderboardAdapter.Row> rows = new ArrayList<>();
                    for (Models.LeaderboardEntry entry : entries) {
                        LeaderboardAdapter.Row row = new LeaderboardAdapter.Row();
                        row.rank = entry.rank;
                        row.name = entry.userName;
                        row.points = entry.points;
                        rows.add(row);
                    }
                    adapter.submit(rows);
                }
            }

            @Override
            public void onError(String error) {
                if (progressIndicator != null) {
                    progressIndicator.setVisibility(View.GONE);
                }
                showEmptyState();
                Toast.makeText(getContext(), "Error loading leaderboard: " + error, Toast.LENGTH_SHORT).show();
                loadSampleLeaderboard();
            }
        });
    }

    private void loadSampleLeaderboard() {
        List<LeaderboardAdapter.Row> sampleData = new ArrayList<>();
        
        // Sample leaderboard data
        String[] names = {"John Smith", "Sarah Johnson", "Michael Chen", "Emily Davis", "David Wilson", "Lisa Brown", "James Miller", "Jennifer Garcia", "Robert Taylor", "Amanda Martinez"};
        int[] points = {1250, 1180, 1050, 980, 920, 850, 780, 720, 680, 650};
        
        for (int i = 0; i < names.length; i++) {
            LeaderboardAdapter.Row row = new LeaderboardAdapter.Row();
            row.rank = i + 1;
            row.name = names[i];
            row.points = points[i];
            sampleData.add(row);
        }
        
        adapter.submit(sampleData);
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLeaderboard();
    }
}
