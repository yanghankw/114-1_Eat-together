package com.example.eat_together;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    private List<AlarmItem> alarmList;
    private OnAlarmLongClickListener longClickListener;

    // 定義一個介面，讓外部（Fragment）可以實作長按後的動作
    public interface OnAlarmLongClickListener {
        void onAlarmLongClick(int position);
    }

    // 建構子：傳入資料清單與監聽器
    public AlarmAdapter(List<AlarmItem> alarmList, OnAlarmLongClickListener listener) {
        this.alarmList = alarmList;
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 載入我們之前建立的 item_alarm.xml 佈局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmItem item = alarmList.get(position);

        // 設定時間、日期與開關狀態
        holder.tvTime.setText(item.time);
        holder.tvDate.setText(item.date);
        holder.switchAlarm.setChecked(item.isOn);

        // --- 🔥 實作長按刪除邏輯 ---
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    // 觸發外部傳進來的長按事件
                    longClickListener.onAlarmLongClick(holder.getAdapterPosition());
                }
                // 回傳 true 代表這個長按事件已被處理，不會再觸發一般點擊
                return true;
            }
        });

        // 也可以順便加上開關的監聽（選配）
        holder.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isOn = isChecked;
            // 這裡未來可以加入：如果關閉開關，就取消系統鬧鐘
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    // ViewHolder：定義畫面上的元件
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 1. 定義元件變數
        TextView tvTime, tvDate;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchAlarm; // 或者使用 CompoundButton switchAlarm 也可以

        // 2. 必須要有這個建構函數
        public ViewHolder(@NonNull View itemView) {
            super(itemView); // 這是最重要的一行，一定要呼叫

            // 3. 在這裡連結 XML 裡的 ID
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            switchAlarm = itemView.findViewById(R.id.switchAlarm);
        }
    }
}