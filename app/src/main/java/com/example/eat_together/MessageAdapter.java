package com.example.eat_together;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;

    public MessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    // 1. 決定這條訊息是「我」、「對方」還是「活動」
    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    // 2. 根據類型載入對應的 XML Layout
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_ME) {
            // 載入 item_msg_me.xml
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_me, parent, false);
            return new MeViewHolder(view);
        } else if (viewType == ChatMessage.TYPE_OTHER) {
            // 載入 item_msg_other.xml
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_other, parent, false);
            return new OtherViewHolder(view);
        } else {
            // 載入 item_msg_event.xml (活動卡片)
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_event, parent, false);
            return new EventViewHolder(view);
        }
    }

    // 3. 綁定資料 (將文字填入 View)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);

        if (holder instanceof MeViewHolder) {
            // --- 處理「我」的訊息 ---
            MeViewHolder meHolder = (MeViewHolder) holder;
            meHolder.tvContent.setText(msg.getContent());
            // 如果您的 Layout 有顯示名字的需求，可以打開下面這行：
            // meHolder.tvSenderName.setText(msg.getName());

        } else if (holder instanceof OtherViewHolder) {
            // --- 處理「對方」的訊息 ---
            OtherViewHolder otherHolder = (OtherViewHolder) holder;
            otherHolder.tvContent.setText(msg.getContent());
            otherHolder.tvSenderName.setText(msg.getName());

        } else if (holder instanceof EventViewHolder) {
            // --- 處理「活動卡片」 ---
            EventViewHolder eventHolder = (EventViewHolder) holder;
            eventHolder.tvTitle.setText(msg.getEventTitle());

            // 時間格式處理：防呆機制，避免字串太短導致當機
            String rawTime = msg.getEventTime();
            if (rawTime != null && rawTime.length() > 16) {
                eventHolder.tvTime.setText("時間：" + rawTime.replace("T", " ").substring(0, 16));
            } else {
                eventHolder.tvTime.setText("時間：" + rawTime);
            }

            // 按鈕點擊事件：發送加入指令
            eventHolder.btnJoin.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "正在加入活動...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    // 發送 JOIN_EVENT 指令給 Server (格式依您後端需求)
                    TcpClient.getInstance().sendMessage("JOIN_EVENT:" + msg.getEventId());
                }).start();
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ================== ViewHolder 定義區 ==================

    // 1. 我的訊息 (對應 item_msg_me.xml)
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvSenderName;

        MeViewHolder(View view) {
            super(view);
            // 根據您的 XML ID 設定
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name);
        }
    }

    // 2. 對方的訊息 (對應 item_msg_other.xml)
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvSenderName;

        OtherViewHolder(View view) {
            super(view);
            // 根據您的 XML ID 設定
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name);
        }
    }

    // 3. 活動卡片 (對應 item_msg_event.xml)
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        Button btnJoin;

        EventViewHolder(View view) {
            super(view);
            // 根據活動卡片 Layout 的 ID 設定
            tvTitle = view.findViewById(R.id.tv_event_title);
            tvTime = view.findViewById(R.id.tv_event_time);
            btnJoin = view.findViewById(R.id.btn_join_event);
        }
    }
}