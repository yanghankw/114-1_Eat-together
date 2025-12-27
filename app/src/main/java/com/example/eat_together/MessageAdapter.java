package com.example.eat_together;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;

    public MessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_ME) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_me, parent, false);
            return new MeViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_other, parent, false);
            return new OtherViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);

        if (holder instanceof MeViewHolder) {
            MeViewHolder meHolder = (MeViewHolder) holder;
            meHolder.tvContent.setText(msg.getContent());
            // ★ 設定自己的名字 (也可以寫死 "我")
            meHolder.tvSenderName.setText(msg.getSenderName());

        } else if (holder instanceof OtherViewHolder) {
            OtherViewHolder otherHolder = (OtherViewHolder) holder;
            otherHolder.tvContent.setText(msg.getContent());
            // ★ 設定對方的名字
            otherHolder.tvSenderName.setText(msg.getSenderName());
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // --- ViewHolder 修改區 ---

    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvSenderName; // ★ 新增

        MeViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name); // ★ 綁定 ID
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvSenderName; // ★ 新增

        OtherViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name); // ★ 綁定 ID
        }
    }
}