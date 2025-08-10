package com.example.looplab.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ConversationsAdapter extends ListAdapter<Models.Conversation, ConversationsAdapter.ViewHolder> {

    private OnConversationClickListener listener;

    public ConversationsAdapter() {
        super(new DiffUtil.ItemCallback<Models.Conversation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Models.Conversation oldItem, @NonNull Models.Conversation newItem) {
                return Objects.equals(oldItem.id, newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Models.Conversation oldItem, @NonNull Models.Conversation newItem) {
                return Objects.equals(oldItem.lastMessage, newItem.lastMessage)
                        && oldItem.lastMessageTime == newItem.lastMessageTime;
            }
        });
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Models.Conversation conversation = getItem(position);
        holder.bind(conversation);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.clearListener();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvName;
        private TextView tvLastMessage;
        private TextView tvTime;
        private TextView tvUnreadCount;
        private View unreadIndicator;
        private com.google.firebase.firestore.ListenerRegistration unreadListener;
        private android.widget.ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationClick(getItem(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                Models.Conversation conv = getItem(position);
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                        .setTitle("Delete chat")
                        .setMessage("This will delete the chat and its messages for you. Continue?")
                        .setPositiveButton("Delete", (d, w) -> {
                            // Delete chat and its messages
                            com.example.looplab.data.ChatService svc = new com.example.looplab.data.ChatService();
                            svc.deleteChat(conv.id, new com.example.looplab.data.ChatService.ChatCallback() {
                                @Override public void onSuccess() { android.widget.Toast.makeText(v.getContext(), "Deleted", android.widget.Toast.LENGTH_SHORT).show(); }
                                @Override public void onError(String error) { android.widget.Toast.makeText(v.getContext(), error, android.widget.Toast.LENGTH_SHORT).show(); }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        public void bind(Models.Conversation conversation) {
            // Remove old listener to avoid leaks when views are recycled
            clearListener();

            String title = (conversation.name != null && !conversation.name.isEmpty()) ? conversation.name : "Chat";
            tvName.setText(title);
            tvLastMessage.setText(conversation.lastMessage != null ? conversation.lastMessage : "No messages yet");

            // Format time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            long messageTime = conversation.lastMessageTime;

            if (messageTime > 0) {
                Date messageDate = new Date(messageTime);
                Date now = new Date();

                // If message is from today, show time, otherwise show date
                if (isSameDay(messageDate, now)) {
                    tvTime.setText(timeFormat.format(messageDate));
                } else {
                    tvTime.setText(dateFormat.format(messageDate));
                }
            } else {
                tvTime.setText("");
            }

            // Set avatar based on conversation type
            if ("ai".equals(conversation.type)) {
                ivAvatar.setImageResource(R.drawable.ic_ai);
            } else if ("group".equals(conversation.type)) {
                ivAvatar.setImageResource(R.drawable.ic_group);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            // Unread indicator: query only on equality filters to avoid index requirements
            final String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            unreadListener = com.example.looplab.data.FirebaseRefs.messages()
                    .whereEqualTo("chatId", conversation.id)
                    .addSnapshotListener((snap, e) -> {
                        int count = 0;
                        if (snap != null) {
                            for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                                Models.Message m = d.toObject(Models.Message.class);
                                if (m != null && !m.isRead && (currentUid == null || !currentUid.equals(m.senderId))) {
                                    count++;
                                }
                            }
                        }
                        if (count > 0) {
                            unreadIndicator.setVisibility(View.VISIBLE);
                            tvUnreadCount.setVisibility(View.VISIBLE);
                            tvUnreadCount.setText(String.valueOf(count));
                        } else {
                            unreadIndicator.setVisibility(View.GONE);
                            tvUnreadCount.setVisibility(View.GONE);
                        }
                    });
        }

        private boolean isSameDay(Date date1, Date date2) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date1).equals(sdf.format(date2));
        }

        public void clearListener() {
            if (unreadListener != null) {
                unreadListener.remove();
                unreadListener = null;
            }
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(Models.Conversation conversation);
    }
}
