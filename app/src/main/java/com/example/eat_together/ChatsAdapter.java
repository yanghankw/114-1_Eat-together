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

// 匯入 Glide
import com.bumptech.glide.Glide;

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

        // --- ★ 關鍵修改：統一使用「名字」產生頭像 ---
        // 原本是用 ID，現在改成 Name，這樣跟好友列表才會一樣
        String seed = session.getName();

        if (seed == null || seed.isEmpty()) {
            seed = "default_user";
        }

        // 組合網址
        String url = "https://robohash.org/" + seed + ".png?set=set1";

        Glide.with(context)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background) // 載入失敗時顯示
                .into(holder.ivAvatar);

        // 點擊事件
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("FRIEND_ID", session.getId());
            intent.putExtra("FRIEND_NAME", session.getName());
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