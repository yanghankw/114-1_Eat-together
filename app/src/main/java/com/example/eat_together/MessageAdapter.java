package com.example.eat_together;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.bumptech.glide.Glide;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;

    public MessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    // ★★★ 關鍵 1：告訴 RecyclerView 有哪幾種 layout ★★★
    @Override
    public int getItemViewType(int position) {
        // 這裡會回傳 0 (自己), 1 (對方), 或 2 (活動卡片)
        return messageList.get(position).getType();
    }

    // ★★★ 關鍵 2：根據類型載入對應的 XML 檔案 ★★★
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == ChatMessage.TYPE_ME) {
            // 載入自己的綠色泡泡
            View view = inflater.inflate(R.layout.item_msg_me, parent, false);
            return new MeViewHolder(view);
        } else if (viewType == ChatMessage.TYPE_OTHER) {
            // 載入對方的白色泡泡
            View view = inflater.inflate(R.layout.item_msg_other, parent, false);
            return new OtherViewHolder(view);
        } else {
            // ★★★ 如果是 TYPE_EVENT，就載入你做的黃色卡片 XML ★★★
            View view = inflater.inflate(R.layout.item_msg_event, parent, false);
            return new EventViewHolder(view);
        }
    }

    // ★★★ 關鍵 3：把資料填進去 ★★★
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);

        // === 情況 A: 處理自己的訊息 (維持原樣) ===
        if (holder instanceof MeViewHolder) {
            MeViewHolder meHolder = (MeViewHolder) holder;
            meHolder.tvContent.setText(msg.getContent());

            String myName = (msg.getSenderName() != null) ? msg.getSenderName() : "我";
            meHolder.tvSenderName.setText(myName);

            String url = "https://robohash.org/" + myName + ".png?set=set1";
            Glide.with(meHolder.itemView.getContext()).load(url).circleCrop().placeholder(R.drawable.ic_launcher_background).into(meHolder.ivAvatar);
        }

        // === 情況 B: 處理對方的訊息 (維持原樣) ===
        else if (holder instanceof OtherViewHolder) {
            OtherViewHolder otherHolder = (OtherViewHolder) holder;
            otherHolder.tvContent.setText(msg.getContent());

            String name = (msg.getSenderName() != null) ? msg.getSenderName() : "對方";
            otherHolder.tvSenderName.setText(name);

            String url = "https://robohash.org/" + name + ".png?set=set1";
            Glide.with(otherHolder.itemView.getContext()).load(url).circleCrop().placeholder(R.drawable.ic_launcher_background).into(otherHolder.ivAvatar);
        }

        // === 情況 C: ★★★ 處理活動卡片 (這裡就是魔法發生的地方) ★★★ ===
        else if (holder instanceof EventViewHolder) {
            EventViewHolder eventHolder = (EventViewHolder) holder;

            // 取得那一長串原始資料，例如："10,McDonald's,2025-12-28T16:39:00+08:00"
            String rawContent = msg.getContent();

            try {
                // 1. 用逗號 "," 把字串切開
                String[] parts = rawContent.split(",");

                // 確保切出來至少有 3 個部分 (ID, 地點, 時間)
                if (parts.length >= 3) {
                    String location = parts[1]; // 第 2 部分是地點 (McDonald's)
                    String timeRaw = parts[2];  // 第 3 部分是時間 (2025-12-28T...)

                    // 2. 美化時間顯示 (把中間的 'T' 換成空白，並只取前 16 個字)
                    String displayTime = timeRaw.replace("T", " ");
                    if(displayTime.length() > 16) displayTime = displayTime.substring(0, 16);

                    // 3. 把整理好的資料填入卡片的格子裡
                    eventHolder.tvTitle.setText(location);
                    eventHolder.tvTime.setText("時間：" + displayTime);
                } else {
                    // 如果資料格式不對，就直接顯示原始資料當作備案
                    eventHolder.tvTitle.setText(rawContent);
                    eventHolder.tvTime.setText("時間格式錯誤");
                }
            } catch (Exception e) {
                // 發生任何錯誤時的備案
                eventHolder.tvTitle.setText("無法讀取活動資料");
                eventHolder.tvTime.setText("");
            }

            // 4. 設定按鈕的點擊事件
            eventHolder.btnJoin.setOnClickListener(v -> {
                // 這裡之後可以寫跳轉到活動詳情頁面的程式碼
                Toast.makeText(v.getContext(), "正在加入： " + eventHolder.tvTitle.getText(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // --- ViewHolder 定義區 (對照 XML 裡的 ID) ---

    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvSenderName;
        ImageView ivAvatar;
        MeViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name);
            ivAvatar = view.findViewById(R.id.iv_avatar);
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvSenderName;
        ImageView ivAvatar;
        OtherViewHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_msg_content);
            tvSenderName = view.findViewById(R.id.tv_sender_name);
            ivAvatar = view.findViewById(R.id.iv_avatar);
        }
    }

    // ★★★ 定義活動卡片的 ViewHolder ★★★
    static class EventViewHolder extends RecyclerView.ViewHolder {
        // 這些變數名稱要對應你的 item_msg_event.xml 裡的 ID
        TextView tvTitle; // 對應 android:id="@+id/tv_event_title"
        TextView tvTime;  // 對應 android:id="@+id/tv_event_time"
        Button btnJoin;   // 對應 android:id="@+id/btn_join_event"

        EventViewHolder(View view) {
            super(view);
            // 綁定 XML 裡的元件
            tvTitle = view.findViewById(R.id.tv_event_title);
            tvTime = view.findViewById(R.id.tv_event_time);
            btnJoin = view.findViewById(R.id.btn_join_event);
        }
    }
}