package com.example.looplab.ui.home.tabs;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.looplab.R;
import com.example.looplab.data.CourseService;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CoursesFragment extends Fragment {
    
    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerViewCourses;
    private LinearLayout emptyState, loadingState;
    
    private CourseService courseService;
    private String currentUserId;
    private List<Models.Course> allCourses;
    private List<Models.Course> filteredCourses;
    private CourseAdapter courseAdapter;
    private String currentFilter = "All";
    private String currentSearch = "";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);
        
        initializeViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSwipeRefresh();
        initializeServices();
        loadCourses();
        
        return view;
    }
    
    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerViewCourses = view.findViewById(R.id.recyclerViewCourses);
        emptyState = view.findViewById(R.id.emptyState);
        loadingState = view.findViewById(R.id.loadingState);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        allCourses = new ArrayList<>();
        filteredCourses = new ArrayList<>();
    }
    
    private void setupToolbar() {
        // Toolbar setup if needed
    }
    
    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(filteredCourses, currentUserId);
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCourses.setAdapter(courseAdapter);
    }
    
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().toLowerCase();
                filterCourses();
            }
        });
    }
    
    private void setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.size() > 0) {
                Chip chip = group.findViewById(checkedIds.get(0));
                currentFilter = chip.getText().toString();
                filterCourses();
            }
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadCourses);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }
    
    private void initializeServices() {
        courseService = new CourseService();
    }
    
    private void loadCourses() {
        showLoading(true);

        if (currentUserId == null) {
            showLoading(false);
            updateEmptyState();
            return;
        }
        
        courseService.getPublishedCourses(new CourseService.CourseListCallback() {
            @Override
            public void onSuccess(List<Models.Course> courses) {
                allCourses.clear();
                allCourses.addAll(courses);
                filterCourses();
                showLoading(false);
                swipeRefresh.setRefreshing(false);
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Failed to load courses: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterCourses() {
        filteredCourses.clear();
        
        for (Models.Course course : allCourses) {
            boolean matchesSearch = currentSearch.isEmpty() || 
                    course.title.toLowerCase().contains(currentSearch) ||
                    course.description.toLowerCase().contains(currentSearch) ||
                    course.instructorName.toLowerCase().contains(currentSearch);
            
            boolean matchesFilter = currentFilter.equals("All") || 
                    course.category.equals(currentFilter);
            
            if (matchesSearch && matchesFilter) {
                filteredCourses.add(course);
            }
        }
        
        courseAdapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (filteredCourses.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewCourses.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewCourses.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLoading(boolean show) {
        if (show) {
            loadingState.setVisibility(View.VISIBLE);
            recyclerViewCourses.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            loadingState.setVisibility(View.GONE);
            recyclerViewCourses.setVisibility(View.VISIBLE);
        }
    }
    
    // Course Adapter Inner Class
    private static class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
        
        private List<Models.Course> courses;
        private String currentUserId;
        
        public CourseAdapter(List<Models.Course> courses, String currentUserId) {
            this.courses = courses;
            this.currentUserId = currentUserId;
        }
        
        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course_enhanced, parent, false);
            return new CourseViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            Models.Course course = courses.get(position);
            holder.bind(course);
        }
        
        @Override
        public int getItemCount() {
            return courses.size();
        }
        
        class CourseViewHolder extends RecyclerView.ViewHolder {
            private TextView tvCourseTitle, tvInstructor, tvLectureCount, tvDuration, tvDifficulty;
            private TextView tvProgressLabel, tvProgressPercent;
            private View progressOverlay;
            private com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
            private com.google.android.material.button.MaterialButton btnEnroll, btnPreview;
            
            public CourseViewHolder(@NonNull View itemView) {
                super(itemView);
                
                tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
                tvInstructor = itemView.findViewById(R.id.tvInstructor);
                tvLectureCount = itemView.findViewById(R.id.tvLectureCount);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
                tvProgressLabel = itemView.findViewById(R.id.tvProgressLabel);
                tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
                progressOverlay = itemView.findViewById(R.id.progressOverlay);
                progressBar = itemView.findViewById(R.id.progressBar);
                btnEnroll = itemView.findViewById(R.id.btnEnroll);
                btnPreview = itemView.findViewById(R.id.btnPreview);
            }
            
            public void bind(Models.Course course) {
                tvCourseTitle.setText(course.title);
                tvInstructor.setText("by " + course.instructorName);
                tvLectureCount.setText(course.lectureCount + " lectures");
                tvDuration.setText("2.5 hours"); // TODO: Calculate actual duration
                tvDifficulty.setText(course.difficulty);
                // Check if user is enrolled
                checkEnrollmentStatus(course);
                
                // Set click listeners
            itemView.setOnClickListener(v -> {
                    android.content.Intent i = new android.content.Intent(itemView.getContext(), com.example.looplab.ui.courses.CourseDetailActivity.class);
                    i.putExtra(com.example.looplab.ui.courses.CourseDetailActivity.EXTRA_COURSE_ID, course.id);
                    itemView.getContext().startActivity(i);
                });
                
                btnEnroll.setOnClickListener(v -> {
                    handleEnrollButton(course);
                });
                
                btnPreview.setOnClickListener(v -> {
                    handlePreviewButton(course);
                });
            }
            
            private void checkEnrollmentStatus(Models.Course course) {
                FirebaseRefs.enrollments().whereEqualTo("userId", currentUserId)
                        .whereEqualTo("courseId", course.id)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // User is enrolled
                                Models.Enrollment enrollment = querySnapshot.getDocuments().get(0)
                                        .toObject(Models.Enrollment.class);
                                if (enrollment != null) {
                                    // Load actual progress from progress data
                                    loadActualCourseProgress(course.id);
                                }
                            } else {
                                // User is not enrolled
                                showEnrollState();
                            }
                        })
                        .addOnFailureListener(e -> {
                            showEnrollState();
                        });
            }
            
            private void loadActualCourseProgress(String courseId) {
                CourseService courseService = new CourseService();
                courseService.getUserProgress(currentUserId, courseId, new CourseService.ProgressCallback() {
                    @Override
                    public void onSuccess(Models.Progress progress) {
                        if (progress.completedLectures != null && progress.completedLectures.size() > 0) {
                            // Get total lectures count for this course
                            courseService.getCourseLectures(courseId, new CourseService.LectureCallback() {
                                @Override
                                public void onSuccess(List<Models.Lecture> lectures) {
                                    int totalLectures = lectures.size();
                                    int completedLectures = progress.completedLectures.size();
                                    int progressPercentage = totalLectures > 0 ? (completedLectures * 100) / totalLectures : 0;
                                    
                                    showEnrolledState(progressPercentage);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    // Fallback to 0% progress
                                    showEnrolledState(0);
                                }
                            });
                        } else {
                            // No completed lectures
                            showEnrolledState(0);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Fallback to 0% progress
                        showEnrolledState(0);
                    }
                });
            }
            
            private void showEnrolledState(int progress) {
                btnEnroll.setText("Continue");
                btnEnroll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.success)));
                
                progressOverlay.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
                tvProgressPercent.setText(progress + "%");
                
                // Update progress bar color based on completion level
                if (progress >= 100) {
                    progressBar.setIndicatorColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                } else if (progress >= 50) {
                    progressBar.setIndicatorColor(itemView.getContext().getColor(android.R.color.holo_blue_dark));
                } else {
                    progressBar.setIndicatorColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
                }
            }
            
            private void showEnrollState() {
                btnEnroll.setText("Enroll");
                btnEnroll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.primary)));
                
                progressOverlay.setVisibility(View.GONE);
            }
            
            private void handleEnrollButton(Models.Course course) {
                if (btnEnroll.getText().equals("Enroll")) {
                    // Enroll in course
                    CourseService courseService = new CourseService();
                    courseService.enrollInCourse(currentUserId, course.id, new CourseService.EnrollmentCallback() {
                        @Override
                        public void onSuccess(Models.Enrollment enrollment) {
                            Toast.makeText(itemView.getContext(), "Enrolled successfully!", Toast.LENGTH_SHORT).show();
                            checkEnrollmentStatus(course); // Refresh UI
                        }
                        
                        @Override
                        public void onError(String error) {
                            Toast.makeText(itemView.getContext(), "Failed to enroll: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Continue course
                    Toast.makeText(itemView.getContext(), "Continue course: " + course.title, Toast.LENGTH_SHORT).show();
                }
            }
            
            private void handlePreviewButton(Models.Course course) {
                android.content.Intent i = new android.content.Intent(itemView.getContext(), com.example.looplab.ui.courses.CoursePreviewActivity.class);
                i.putExtra(com.example.looplab.ui.courses.CoursePreviewActivity.EXTRA_COURSE_ID, course.id);
                itemView.getContext().startActivity(i);
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh courses when returning to the fragment
        loadCourses();
    }
}


