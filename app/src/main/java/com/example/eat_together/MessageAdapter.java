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

    // 關鍵：告訴 RecyclerView 這一行是哪種 Type (自己還是對方)
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
            ((MeViewHolder) holder).tvContent.setText(msg.getContent());
        } else {
            ((OtherViewHolder) holder).tvContent.setText(msg.getContent());
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // 兩個不同的 ViewHolder
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        MeViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        OtherViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
        }
    }
}