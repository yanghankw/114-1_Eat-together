package com.example.eat_together;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlarmFragment extends Fragment {

    private AlarmAdapter adapter;
    private List<AlarmItem> alarmList;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. 載入佈局
        View view = inflater.inflate(R.layout.activity_alarm, container, false);

        // 2. 初始化 UI 元件
        RecyclerView rvAlarms = view.findViewById(R.id.rvAlarms);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        sharedPreferences = getActivity().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);

        // 3. 檢查通知權限 (針對 Android 13+)
        checkNotificationPermission();

        // 4. 初始化列表與資料
        alarmList = new ArrayList<>();
        loadAlarmsFromPrefs();

        // 5. 設定 Adapter 並實作「長按刪除」介面
        adapter = new AlarmAdapter(alarmList, position -> showDeleteConfirmDialog(position));
        rvAlarms.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAlarms.setAdapter(adapter);

        // 6. 設定「+」按鈕點擊事件
        fabAdd.setOnClickListener(v -> showTimePicker());

        return view;
    }

    // 彈出時間選擇器
    private void showTimePicker() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
            addNewAlarm(hourOfDay, minuteOfHour);
        }, hour, minute, false).show();
    }

    // 新增鬧鐘邏輯
    private void addNewAlarm(int hour, int minute) {
        // 生成唯一 ID
        int id = (int) System.currentTimeMillis();

        // 格式化顯示時間
        String amPm = (hour < 12) ? "上午" : "下午";
        int displayHour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);
        String timeStr = String.format("%s %02d:%02d", amPm, displayHour, minute);
        String dateStr = "12月21日，週日"; // 這裡可根據實際需求抓取日期

        // 加入列表
        AlarmItem newItem = new AlarmItem(id, timeStr, dateStr, true);
        alarmList.add(newItem);
        adapter.notifyItemInserted(alarmList.size() - 1);

        // 儲存並設定系統計時
        saveAlarmsToPrefs();
        scheduleSystemAlarm(newItem, hour, minute);

        Toast.makeText(getContext(), "已設定鬧鐘：" + timeStr, Toast.LENGTH_SHORT).show();
    }

    // 設定系統 AlarmManager
    private void scheduleSystemAlarm(AlarmItem item, int hour, int minute) {
        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

        // Android 12+ 精準鬧鐘權限檢查，防止閃退
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        // 使用 item.id 作為 RequestCode，這樣才能精準取消
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), item.id, intent, PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        }
    }

    // 彈出刪除確認對話框
    private void showDeleteConfirmDialog(int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("刪除鬧鐘")
                .setMessage("確定要取消並刪除這個提醒嗎？")
                .setPositiveButton("確定", (dialog, which) -> deleteAlarm(position))
                .setNegativeButton("取消", null)
                .show();
    }

    // 執行刪除
    private void deleteAlarm(int position) {
        AlarmItem itemToRemove = alarmList.get(position);

        // 1. 取消系統計時
        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), itemToRemove.id, intent, PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);

        // 2. 從列表移除
        alarmList.remove(position);
        adapter.notifyItemRemoved(position);

        // 3. 更新儲存
        saveAlarmsToPrefs();
        Toast.makeText(getContext(), "鬧鐘已刪除", Toast.LENGTH_SHORT).show();
    }

    private void saveAlarmsToPrefs() {
        Set<String> alarmSet = new HashSet<>();
        for (AlarmItem item : alarmList) {
            // 格式：ID|時間|日期|開關
            alarmSet.add(item.id + "|" + item.time + "|" + item.date + "|" + item.isOn);
        }
        sharedPreferences.edit().putStringSet("alarm_list_data", alarmSet).apply();
    }

    private void loadAlarmsFromPrefs() {
        Set<String> alarmSet = sharedPreferences.getStringSet("alarm_list_data", null);
        if (alarmSet != null) {
            for (String s : alarmSet) {
                String[] p = s.split("\\|");
                if (p.length == 4) {
                    alarmList.add(new AlarmItem(Integer.parseInt(p[0]), p[1], p[2], Boolean.parseBoolean(p[3])));
                }
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}