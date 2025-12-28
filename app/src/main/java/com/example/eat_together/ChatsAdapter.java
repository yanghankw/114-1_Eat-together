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


        holder.itemView.setOnClickListener(v -> {
            android.util.Log.d("AdapterDebug", "點擊了: " + session.getName() + ", 類型是: " + session.getType());
            Intent intent = new Intent(context, ChatActivity.class);

            // 1. 傳送 ID (給 ChatActivity 的 targetId)
            intent.putExtra("FRIEND_ID", session.getId());

            // 2. 傳送 名稱 (給 ChatActivity 的標題)
            intent.putExtra("FRIEND_NAME", session.getName());

            // 3. ★ 最重要的一步：傳送聊天類型 (ChatActivity 才能決定要不要顯示邀請按鈕)
            // 假設你的 ChatSession 有一個 getType() 方法回傳 "GROUP" 或 "PRIVATE"
            intent.putExtra("CHAT_TYPE", session.getType());

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