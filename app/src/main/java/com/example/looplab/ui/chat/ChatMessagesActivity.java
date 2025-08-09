package com.example.looplab.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.ChatService;
import com.example.looplab.data.model.Models;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class ChatMessagesActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chat_id";
    public static final String EXTRA_CHAT_NAME = "chat_name";

    private String chatId;
    private String chatName;
    private String currentUserId;
    private ChatService chatService;

    private RecyclerView rv;
    private MessagesAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private ListenerRegistration messagesListener;
    private android.widget.TextView tvEmpty;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_messages);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        chatName = getIntent().getStringExtra(EXTRA_CHAT_NAME);
        currentUserId = FirebaseAuth.getInstance().getUid();
        chatService = new ChatService();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(chatName != null ? chatName : "Chat");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvEmpty = findViewById(R.id.tvEmpty);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new MessagesAdapter();
        rv.setAdapter(adapter);

        btnSend.setOnClickListener(v -> send());

        startListening();

        // Ensure window adjusts for keyboard
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void startListening() {
        stopListening();
        messagesListener = chatService.listenToChatMessages(chatId, new ChatService.MessageCallback() {
            @Override public void onSuccess(List<Models.Message> messages) {
                adapter.submit(messages);
                tvEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                rv.scrollToPosition(Math.max(0, adapter.getItemCount()-1));
                // Mark as read for current user
                chatService.markMessagesAsRead(chatId, currentUserId, new ChatService.ChatCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(String error) {}
                });
            }
            @Override public void onError(String error) {
                Snackbar.make(rv, error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void stopListening() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void send() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etMessage.setText("");
        chatService.sendMessage(chatId, currentUserId, text, "text", new ChatService.ChatCallback() {
            @Override public void onSuccess() { /* listener will update UI */ }
            @Override public void onError(String error) { Snackbar.make(rv, error, Snackbar.LENGTH_LONG).show(); }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListening();
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {
        private final List<Models.Message> data = new ArrayList<>();
        void submit(List<Models.Message> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) { return new VH(android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_message, p, false)); }
        @Override public void onBindViewHolder(VH h, int i) { h.bind(data.get(i)); }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            private final View myBubble, otherBubble, systemBubble; 
            private final android.widget.TextView myText, otherText, myTime, otherTime, systemText;
            VH(View itemView){ 
                super(itemView); 
                myBubble=itemView.findViewById(R.id.myBubble); 
                otherBubble=itemView.findViewById(R.id.otherBubble); 
                systemBubble=itemView.findViewById(R.id.systemBubble);
                myText=itemView.findViewById(R.id.tvMyText); 
                otherText=itemView.findViewById(R.id.tvOtherText); 
                myTime=itemView.findViewById(R.id.tvMyTime); 
                otherTime=itemView.findViewById(R.id.tvOtherTime);
                systemText=itemView.findViewById(R.id.tvSystemText);
            }
            void bind(Models.Message m){ 
                boolean isSystem = "system".equals(m.type);
                boolean isMine = FirebaseAuth.getInstance().getUid()!=null && FirebaseAuth.getInstance().getUid().equals(m.senderId);
                myBubble.setVisibility(!isSystem && isMine?View.VISIBLE:View.GONE); 
                otherBubble.setVisibility(!isSystem && !isMine?View.VISIBLE:View.GONE);
                systemBubble.setVisibility(isSystem?View.VISIBLE:View.GONE);
                if (isSystem) {
                    systemText.setText(m.content);
                } else if (isMine){ 
                    myText.setText(m.content); 
                    myTime.setText(DateFormat.getTimeInstance().format(m.timestamp)); 
                } else { 
                    otherText.setText(m.content); 
                    otherTime.setText(DateFormat.getTimeInstance().format(m.timestamp)); 
                } 
            }
        }
    }
}


