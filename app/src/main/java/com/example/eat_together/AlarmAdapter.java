package com.example.eat_together;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    private List<AlarmItem> alarmList;
    private OnAlarmLongClickListener longClickListener;
    private OnAlarmStatusChangeListener statusChangeListener; // 🔥 新增監聽器

    public interface OnAlarmLongClickListener {
        void onAlarmLongClick(int position);
    }

    // 🔥 定義狀態改變介面
    public interface OnAlarmStatusChangeListener {
        void onStatusChange(int position, boolean isChecked);
    }

    // 🔥 修改建構子
    public AlarmAdapter(List<AlarmItem> alarmList, OnAlarmLongClickListener longListener, OnAlarmStatusChangeListener statusListener) {
        this.alarmList = alarmList;
        this.longClickListener = longListener;
        this.statusChangeListener = statusListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmItem item = alarmList.get(position);

        holder.tvTime.setText(item.time);
        holder.tvDate.setText(item.date);

        // 🔥 先移除監聽器再設定狀態，避免 RecyclerView 回收機制觸發錯誤邏輯
        holder.switchAlarm.setOnCheckedChangeListener(null);
        holder.switchAlarm.setChecked(item.isOn);

        // 🔥 設定開關監聽
        holder.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isOn = isChecked;
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange(holder.getAdapterPosition(), isChecked);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onAlarmLongClick(holder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() { return alarmList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDate;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switchAlarm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            switchAlarm = itemView.findViewById(R.id.switchAlarm);
        }
    }
}