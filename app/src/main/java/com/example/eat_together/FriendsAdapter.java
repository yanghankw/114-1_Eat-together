package com.example.eat_together;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // ★ 記得 import
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<Friend> friendList;
    private String myUserId; // ★ 1. 新增：紀錄目前登入者的 ID

    // ★ 2. 修改建構子：傳入資料來源 + 自己的 ID
    public FriendsAdapter(List<Friend> friendList, String myUserId) {
        this.friendList = friendList;
        this.myUserId = myUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.tvName.setText(friend.getName());
        holder.tvStatus.setText(friend.getEmail());
        holder.ivAvatar.setImageResource(friend.getImageResId());

        // ★ 3. 設定按鈕點擊事件
        holder.btnAdd.setOnClickListener(v -> {
            // A. UI 立即反應 (變灰色、文字改成已加入，避免重複點擊)
            holder.btnAdd.setText("已加入");
            holder.btnAdd.setEnabled(false);
            holder.btnAdd.setBackgroundColor(0xFF888888); // 設定成灰色

            // B. 背景發送網路請求
            sendAddFriendCommand(friend.getId());
        });
    }

    // 輔助方法：發送指令給 Server
    private void sendAddFriendCommand(String targetFriendId) {
        new Thread(() -> {
            // 確保雙方 ID 都存在
            if (myUserId != null && targetFriendId != null) {
                // 指令格式: ADD_FRIEND:我的ID:對方ID
                String cmd = "ADD_FRIEND:" + myUserId + ":" + targetFriendId;
                TcpClient.getInstance().sendMessage(cmd);
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    // ViewHolder 類別
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        ImageView ivAvatar;
        Button btnAdd; // ★ 4. 新增按鈕元件

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            ivAvatar = itemView.findViewById(R.id.iv_friend_avatar); // XML原本叫 iv_friend_avatar，請確認 ID 是否一致

            // 綁定 XML 裡的按鈕 ID
            btnAdd = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}