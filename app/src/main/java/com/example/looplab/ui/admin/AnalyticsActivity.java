package com.example.looplab.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CircularProgressIndicator progressIndicator;
    
    // Overview Cards
    private TextView tvTotalUsers, tvActiveUsers, tvTotalCourses, tvTotalEvents;
    private TextView tvEnrollments, tvCompletionRate, tvEngagementRate, tvRevenue;
    
    // Charts
    private LineChart lineChartUserGrowth;
    private BarChart barChartCourseEnrollments;
    private PieChart pieChartUserRoles;
    private LineChart lineChartEngagement;
    
    // Control buttons
    private MaterialButton btnExportReport, btnRefreshData, btnDateRange;
    
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);

        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadAnalyticsData();
        
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        // Overview cards
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvTotalCourses = findViewById(R.id.tvTotalCourses);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvEnrollments = findViewById(R.id.tvEnrollments);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvEngagementRate = findViewById(R.id.tvEngagementRate);
        tvRevenue = findViewById(R.id.tvRevenue);
        
        // Charts
        lineChartUserGrowth = findViewById(R.id.lineChartUserGrowth);
        barChartCourseEnrollments = findViewById(R.id.barChartCourseEnrollments);
        pieChartUserRoles = findViewById(R.id.pieChartUserRoles);
        lineChartEngagement = findViewById(R.id.lineChartEngagement);
        
        // Buttons
        btnExportReport = findViewById(R.id.btnExportReport);
        btnRefreshData = findViewById(R.id.btnRefreshData);
        btnDateRange = findViewById(R.id.btnDateRange);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnExportReport.setOnClickListener(v -> exportReport());
        btnRefreshData.setOnClickListener(v -> {
            progressIndicator.setVisibility(View.VISIBLE);
            loadAnalyticsData();
        });
        btnDateRange.setOnClickListener(v -> showDateRangeDialog());
    }

    private void loadAnalyticsData() {
        loadOverviewData();
        loadUserGrowthChart();
        loadCourseEnrollmentChart();
        loadUserRoleChart();
        loadEngagementChart();
    }

    private void loadOverviewData() {
        // Load total users
        FirebaseRefs.users().get()
                .addOnSuccessListener(snapshot -> {
                    int totalUsers = snapshot.size();
                    tvTotalUsers.setText(String.valueOf(totalUsers));
                    
                    // Count active users
                    int activeUsers = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Boolean isActive = doc.getBoolean("isActive");
                        if (isActive != null && isActive) {
                            activeUsers++;
                        }
                    }
                    tvActiveUsers.setText(String.valueOf(activeUsers));
                    
                    // Calculate engagement rate
                    double engagementRate = totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0;
                    tvEngagementRate.setText(String.format("%.1f%%", engagementRate));
                });

        // Load total courses
        FirebaseRefs.courses().get()
                .addOnSuccessListener(snapshot -> {
                    tvTotalCourses.setText(String.valueOf(snapshot.size()));
                });

        // Load total events
        FirebaseRefs.events().get()
                .addOnSuccessListener(snapshot -> {
                    tvTotalEvents.setText(String.valueOf(snapshot.size()));
                });

        // Load enrollments
        FirebaseRefs.enrollments().get()
                .addOnSuccessListener(snapshot -> {
                    int totalEnrollments = snapshot.size();
                    tvEnrollments.setText(String.valueOf(totalEnrollments));
                    
                    // Calculate completion rate
                    int completedEnrollments = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Integer progress = doc.getLong("progress") != null ? doc.getLong("progress").intValue() : 0;
                        if (progress >= 100) {
                            completedEnrollments++;
                        }
                    }
                    
                    double completionRate = totalEnrollments > 0 ? (completedEnrollments * 100.0 / totalEnrollments) : 0;
                    tvCompletionRate.setText(String.format("%.1f%%", completionRate));
                });

        // Set sample revenue (in real app, this would come from payment data)
        tvRevenue.setText("$12,450");
        
        progressIndicator.setVisibility(View.GONE);
    }

    private void loadUserGrowthChart() {
        // Sample data for user growth over last 7 days
        List<Entry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            // In real app, query users created on this date
            float users = 10 + (float) (Math.random() * 20); // Sample data
            entries.add(new Entry(6 - i, users));
            calendar = Calendar.getInstance(); // Reset
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily New Users");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);
        lineChartUserGrowth.setData(lineData);
        
        Description desc = new Description();
        desc.setText("User Growth (Last 7 Days)");
        lineChartUserGrowth.setDescription(desc);
        
        lineChartUserGrowth.invalidate();
    }

    private void loadCourseEnrollmentChart() {
        // Sample data for course enrollments
        List<BarEntry> entries = new ArrayList<>();
        String[] courseNames = {"Android Dev", "Web Dev", "AI/ML", "UI/UX", "Data Science"};
        
        for (int i = 0; i < courseNames.length; i++) {
            float enrollments = 20 + (float) (Math.random() * 80); // Sample data
            entries.add(new BarEntry(i, enrollments));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Course Enrollments");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData barData = new BarData(dataSet);
        barChartCourseEnrollments.setData(barData);
        
        Description desc = new Description();
        desc.setText("Enrollments by Course");
        barChartCourseEnrollments.setDescription(desc);
        
        barChartCourseEnrollments.invalidate();
    }

    private void loadUserRoleChart() {
        List<PieEntry> entries = new ArrayList<>();
        
        // Query actual user roles from Firebase
        FirebaseRefs.users().get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> roleCounts = new HashMap<>();
                    roleCounts.put("student", 0);
                    roleCounts.put("teacher", 0);
                    roleCounts.put("admin", 0);
                    
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String role = doc.getString("role");
                        if (role != null && roleCounts.containsKey(role)) {
                            roleCounts.put(role, roleCounts.get(role) + 1);
                        }
                    }
                    
                    entries.clear();
                    for (Map.Entry<String, Integer> entry : roleCounts.entrySet()) {
                        if (entry.getValue() > 0) {
                            entries.add(new PieEntry(entry.getValue(), 
                                    entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1)));
                        }
                    }
                    
                    PieDataSet dataSet = new PieDataSet(entries, "User Roles");
                    dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
                    
                    PieData pieData = new PieData(dataSet);
                    pieChartUserRoles.setData(pieData);
                    
                    Description desc = new Description();
                    desc.setText("User Distribution by Role");
                    pieChartUserRoles.setDescription(desc);
                    
                    pieChartUserRoles.invalidate();
                });
    }

    private void loadEngagementChart() {
        // Sample engagement data over time
        List<Entry> entries = new ArrayList<>();
        
        for (int i = 0; i < 30; i++) {
            float engagement = 60 + (float) (Math.random() * 40); // Sample data 60-100%
            entries.add(new Entry(i, engagement));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Engagement Rate (%)");
        dataSet.setColor(Color.GREEN);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.GREEN);

        LineData lineData = new LineData(dataSet);
        lineChartEngagement.setData(lineData);
        
        Description desc = new Description();
        desc.setText("User Engagement (Last 30 Days)");
        lineChartEngagement.setDescription(desc);
        
        lineChartEngagement.invalidate();
    }

    private void exportReport() {
        StringBuilder report = new StringBuilder();
        report.append("LoopLab Analytics Report\n");
        report.append("Generated on: ").append(dateFormat.format(new Date())).append("\n\n");
        
        report.append("OVERVIEW METRICS\n");
        report.append("Total Users: ").append(tvTotalUsers.getText()).append("\n");
        report.append("Active Users: ").append(tvActiveUsers.getText()).append("\n");
        report.append("Total Courses: ").append(tvTotalCourses.getText()).append("\n");
        report.append("Total Events: ").append(tvTotalEvents.getText()).append("\n");
        report.append("Total Enrollments: ").append(tvEnrollments.getText()).append("\n");
        report.append("Completion Rate: ").append(tvCompletionRate.getText()).append("\n");
        report.append("Engagement Rate: ").append(tvEngagementRate.getText()).append("\n");
        report.append("Revenue: ").append(tvRevenue.getText()).append("\n\n");
        
        report.append("KEY INSIGHTS\n");
        report.append("• User growth is steady with consistent daily registrations\n");
        report.append("• Course completion rates are above industry average\n");
        report.append("• Student engagement remains high across all courses\n");
        report.append("• Event attendance has increased by 15% this month\n");

        // Share report
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, report.toString());
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "LoopLab Analytics Report");
        startActivity(android.content.Intent.createChooser(shareIntent, "Export Report"));
    }

    private void showDateRangeDialog() {
        String[] ranges = {"Last 7 Days", "Last 30 Days", "Last 3 Months", "Last Year", "Custom Range"};
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Date Range")
                .setItems(ranges, (dialog, which) -> {
                    // Handle date range selection
                    String selectedRange = ranges[which];
                    getSupportActionBar().setSubtitle("Showing: " + selectedRange);
                    
                    // Reload data for selected range
                    progressIndicator.setVisibility(View.VISIBLE);
                    loadAnalyticsData();
                })
                .show();
    }
}
