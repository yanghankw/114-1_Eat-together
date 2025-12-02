package com.example.eat_together;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<Friend> friendList;

    // 建構子：傳入資料來源
    public FriendsAdapter(List<Friend> friendList) {
        this.friendList = friendList;
    }

    // 1. 建立每一行的畫面 (只會執行幾次，夠填滿螢幕就好)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    // 2. 把資料綁定到畫面上 (滑動時會一直執行)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.tvName.setText(friend.getName());
        holder.tvStatus.setText(friend.getStatus());
        holder.ivAvatar.setImageResource(friend.getAvatarResId());

        // 這裡可以設定點擊事件 (例如點擊聊天)
        holder.itemView.setOnClickListener(v -> {
            // TODO: 跳轉到聊天室
        });
    }

    // 3. 告訴列表總共有幾筆資料
    @Override
    public int getItemCount() {
        return friendList.size();
    }

    // 內部類別：用來暫存畫面元件，提升效能
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            ivAvatar = itemView.findViewById(R.id.iv_friend_avatar);
        }
    }
}