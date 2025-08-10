package com.example.looplab.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.FirebaseRefs;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class LiveSessionsListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CircularProgressIndicator progress;
    private TextView tvEmpty;
    private SessionsAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live_sessions_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvSessions);
        progress = findViewById(R.id.progressIndicator);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionsAdapter(item -> join(item));
        rv.setAdapter(adapter);

        loadSessions();
    }

    private void loadSessions() {
        progress.setVisibility(View.VISIBLE);
        FirebaseRefs.liveSessions().whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snap, e) -> {
                    progress.setVisibility(View.GONE);
                    if (snap == null || e != null) { tvEmpty.setVisibility(View.VISIBLE); return; }
                    List<SessionRow> rows = new ArrayList<>();
                    for (var d : snap.getDocuments()) {
                        SessionRow r = new SessionRow();
                        r.id = d.getId();
                        r.title = d.getString("title");
                        r.subtitle = d.getString("instructorName");
                        Long ts = d.getLong("createdAt");
                        r.meta = ts != null ? DateFormat.getDateTimeInstance().format(ts) : "";
                        rows.add(r);
                    }
                    tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.submit(rows);
                });
    }

    private void join(SessionRow row) {
        Intent i = new Intent(this, LiveSessionActivity.class);
        startActivity(i);
    }

    static class SessionRow {
        String id; String title; String subtitle; String meta;
    }

    static class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH> {
        interface Listener { void onJoin(SessionRow row); }
        private final List<SessionRow> data = new ArrayList<>();
        private final Listener listener;
        SessionsAdapter(Listener l) { this.listener = l; }
        void submit(List<SessionRow> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_live_session, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final TextView tvTitle; private final TextView tvSub; private final TextView tvMeta; private final com.google.android.material.button.MaterialButton btnJoin;
            VH(View itemView) { super(itemView); tvTitle=itemView.findViewById(R.id.tvTitle); tvSub=itemView.findViewById(R.id.tvSubtitle); tvMeta=itemView.findViewById(R.id.tvMeta); btnJoin=itemView.findViewById(R.id.btnJoin); }
            void bind(SessionRow r, Listener l){ tvTitle.setText(r.title); tvSub.setText(r.subtitle); tvMeta.setText(r.meta); btnJoin.setOnClickListener(v-> l.onJoin(r)); }
        }
    }
}


