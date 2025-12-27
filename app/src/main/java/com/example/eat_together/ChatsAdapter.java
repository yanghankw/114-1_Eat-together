package com.example.eat_together;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

    private List<ChatSession> chatList;
    private Context context;

    public ChatsAdapter(Context context, List<ChatSession> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = chatList.get(position);
        holder.tvName.setText(session.getName());
        holder.tvLastMsg.setText(session.getLastMessage());
        holder.tvTime.setText(session.getTime());
        holder.ivAvatar.setImageResource(session.getAvatarResId());

        // 設定點擊事件
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class); // 假設您的聊天頁面叫 ChatActivity

            // ★ 關鍵：把對方的 ID 和名字都傳過去
            intent.putExtra("FRIEND_ID", session.getFriendId());
            intent.putExtra("FRIEND_NAME", session.getName());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
            ivAvatar = itemView.findViewById(R.id.iv_chat_avatar);
        }
    }
}