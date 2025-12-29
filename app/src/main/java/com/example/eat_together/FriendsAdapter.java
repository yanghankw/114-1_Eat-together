package com.example.eat_together;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 匯入 Glide
import com.bumptech.glide.Glide;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<Friend> friendList;
    private String myUserId;
    private Context context;

    // 定義儲存檔案的名稱
    private static final String PREFS_NAME = "FriendRequestsPrefs";
    private static final String KEY_SENT_REQUESTS = "sent_requests";

    public FriendsAdapter(List<Friend> friendList, String myUserId) {
        this.friendList = friendList;
        this.myUserId = myUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.tvName.setText(friend.getName());
        holder.tvStatus.setText(friend.getEmail());

        // --- ★ 關鍵修改：統一使用「名字」產生頭像 ---
        String seed = friend.getName();

        // 防呆：如果名字是空的，才用 Email
        if (seed == null || seed.isEmpty()) {
            seed = friend.getEmail();
        }
        if (seed == null) seed = "default"; // 最終防呆

        // 組合網址
        String url = "https://robohash.org/" + seed + ".png?set=set1";

        Glide.with(holder.itemView.getContext())
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivAvatar);

        // --- 檢查按鈕狀態 (維持之前的邏輯) ---
        if (checkIfRequestSent(friend.getId())) {
            holder.btnAdd.setText("已加入");
            holder.btnAdd.setEnabled(false);
            holder.btnAdd.setBackgroundColor(0xFF888888);
        } else {
            holder.btnAdd.setText("加入");
            holder.btnAdd.setEnabled(true);
            // 如果你有預設顏色，可以在這裡還原
            // holder.btnAdd.setBackgroundColor(...);
        }

        // --- 點擊事件 ---
        holder.btnAdd.setOnClickListener(v -> {
            saveRequestSent(friend.getId());

            holder.btnAdd.setText("已加入");
            holder.btnAdd.setEnabled(false);
            holder.btnAdd.setBackgroundColor(0xFF888888);

            sendAddFriendCommand(friend.getId());
        });
    }

    // --- 輔助方法 ---
    private boolean checkIfRequestSent(String friendId) {
        if (context == null || friendId == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> sentRequests = prefs.getStringSet(KEY_SENT_REQUESTS, new HashSet<>());
        return sentRequests.contains(friendId);
    }

    private void saveRequestSent(String friendId) {
        if (context == null || friendId == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> oldSet = prefs.getStringSet(KEY_SENT_REQUESTS, new HashSet<>());
        Set<String> newSet = new HashSet<>(oldSet);
        newSet.add(friendId);
        prefs.edit().putStringSet(KEY_SENT_REQUESTS, newSet).apply();
    }

    private void sendAddFriendCommand(String targetFriendId) {
        new Thread(() -> {
            if (myUserId != null && targetFriendId != null) {
                String cmd = "ADD_FRIEND:" + myUserId + ":" + targetFriendId;
                if (TcpClient.getInstance() != null) {
                    TcpClient.getInstance().sendMessage(cmd);
                }
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        ImageView ivAvatar;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            ivAvatar = itemView.findViewById(R.id.iv_friend_avatar);
            btnAdd = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}