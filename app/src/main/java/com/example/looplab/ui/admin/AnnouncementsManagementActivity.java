package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementsManagementActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CircularProgressIndicator progress;
    private TextView tvEmpty;
    private AnnouncementsAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_announcements_management);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvAnnouncements);
        progress = findViewById(R.id.progressIndicator);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnouncementsAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showCreateDialog());

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        progress.setVisibility(View.VISIBLE);
        FirebaseRefs.announcements().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    progress.setVisibility(View.GONE);
                    if (snap == null || e != null) { tvEmpty.setVisibility(View.VISIBLE); return; }
                    List<Row> rows = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Models.Announcement a = d.toObject(Models.Announcement.class);
                        if (a != null) {
                            Row r = new Row();
                            r.id = d.getId();
                            r.title = a.title;
                            r.subtitle = a.createdByName;
                            r.meta = DateFormat.getDateTimeInstance().format(a.createdAt);
                            rows.add(r);
                        }
                    }
                    tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.submit(rows);
                });
    }

    private void showCreateDialog() {
        final EditText etTitle = new EditText(this);
        etTitle.setHint("Title");
        final EditText etBody = new EditText(this);
        etBody.setHint("Message");
        etBody.setMinLines(3);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(etTitle);
        layout.addView(etBody);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Create Announcement")
                .setView(layout)
                .setPositiveButton("Create", (d, w) -> {
                    Models.Announcement a = new Models.Announcement();
                    a.id = FirebaseRefs.announcements().document().getId();
                    a.title = etTitle.getText().toString().trim();
                    a.body = etBody.getText().toString().trim();
                    a.createdAt = System.currentTimeMillis();
                    a.createdBy = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    a.createdByName = "Admin";
                    FirebaseRefs.announcements().document(a.id).set(a.toMap())
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Announcement created", Toast.LENGTH_SHORT).show();
                                // Optional: push via FCM topic
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("announcements");
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class Row { String id; String title; String subtitle; String meta; }

    static class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.VH> {
        private final List<Row> data = new ArrayList<>();
        void submit(List<Row> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_announcement_admin, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i)); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final TextView tvTitle, tvSub, tvMeta; private final View btnDelete;
            VH(View itemView){ super(itemView); tvTitle=itemView.findViewById(R.id.tvTitle); tvSub=itemView.findViewById(R.id.tvSubtitle); tvMeta=itemView.findViewById(R.id.tvMeta); btnDelete=itemView.findViewById(R.id.btnDelete); }
            void bind(Row r){ tvTitle.setText(r.title); tvSub.setText(r.subtitle); tvMeta.setText(r.meta); btnDelete.setOnClickListener(v-> FirebaseRefs.announcements().document(r.id).delete()); }
        }
    }
}


