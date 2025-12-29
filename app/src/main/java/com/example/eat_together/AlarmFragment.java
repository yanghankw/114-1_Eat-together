package com.example.eat_together;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        View view = inflater.inflate(R.layout.activity_alarm, container, false);
        RecyclerView rvAlarms = view.findViewById(R.id.rvAlarms);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        sharedPreferences = getActivity().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);

        checkNotificationPermission();
        alarmList = new ArrayList<>();
        loadAlarmsFromPrefs();

        // 🔥 初始化 Adapter：傳入長按與狀態切換邏輯
        adapter = new AlarmAdapter(alarmList,
                position -> showDeleteConfirmDialog(position),
                (position, isChecked) -> handleToggleAlarm(position, isChecked)
        );

        rvAlarms.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAlarms.setAdapter(adapter);
        fabAdd.setOnClickListener(v -> showDateTimePicker());
        return view;
    }

    // 🔥 處理開關切換：連動系統 AlarmManager
    private void handleToggleAlarm(int position, boolean isChecked) {
        AlarmItem item = alarmList.get(position);
        if (isChecked) {
            // 開啟：重新預約鬧鐘
            scheduleSystemAlarm(item, item.year, item.month, item.day, item.hour, item.minute);
            Toast.makeText(getContext(), "鬧鐘已開啟", Toast.LENGTH_SHORT).show();
        } else {
            // 關閉：取消系統鬧鐘
            cancelSystemAlarm(item);
            Toast.makeText(getContext(), "鬧鐘已關閉", Toast.LENGTH_SHORT).show();
        }
        saveAlarmsToPrefs();
    }

    private void cancelSystemAlarm(AlarmItem item) {
        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), item.id, intent, PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, y, m, d) -> {
            new TimePickerDialog(getContext(), (timeView, h, min) -> {
                // 時間選完後，跳出輸入描述的對話框
                showDescriptionDialog(y, m, d, h, min);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDescriptionDialog(int year, int month, int day, int hour, int minute) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("輸入鬧鐘描述");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("確定", (dialog, which) -> {
            String description = input.getText().toString();
            addNewAlarm(year, month, day, hour, minute, description);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addNewAlarm(int year, int month, int day, int hour, int minute, String description) {
        int id = (int) System.currentTimeMillis();
        String amPm = (hour < 12) ? "上午" : "下午";
        int displayHour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);
        String timeStr = String.format("%s %02d:%02d", amPm, displayHour, minute);
        String dateStr = String.format("%d月%d日", month + 1, day);

        // 🔥 存入完整數值，包含描述
        AlarmItem newItem = new AlarmItem(id, timeStr, dateStr, true, year, month, day, hour, minute, description);
        alarmList.add(newItem);
        adapter.notifyItemInserted(alarmList.size() - 1);

        saveAlarmsToPrefs();
        scheduleSystemAlarm(newItem, year, month, day, hour, minute);
    }

    private void scheduleSystemAlarm(AlarmItem item, int year, int month, int day, int hour, int minute) {
        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                return;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        if (calendar.before(Calendar.getInstance())) calendar.add(Calendar.DATE, 1);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        // 將描述傳遞給 Receiver
        intent.putExtra("description", item.description);
        PendingIntent pi = PendingIntent.getBroadcast(getContext(), item.id, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
    }

    private void showDeleteConfirmDialog(int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("刪除鬧鐘").setMessage("確定要刪除嗎？")
                .setPositiveButton("確定", (dialog, which) -> {
                    cancelSystemAlarm(alarmList.get(position));
                    alarmList.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveAlarmsToPrefs();
                }).setNegativeButton("取消", null).show();
    }

    // 🔥 修改儲存格式：加入描述欄位
    private void saveAlarmsToPrefs() {
        Set<String> alarmSet = new HashSet<>();
        for (AlarmItem item : alarmList) {
            // 注意：如果 description 包含 | 符號可能會出錯，建議做簡單處理
            String safeDesc = (item.description == null) ? "" : item.description.replace("|", " ");
            alarmSet.add(item.id + "|" + item.time + "|" + item.date + "|" + item.isOn + "|"
                    + item.year + "|" + item.month + "|" + item.day + "|" + item.hour + "|" + item.minute + "|" + safeDesc);
        }
        sharedPreferences.edit().putStringSet("alarm_list_data", alarmSet).apply();
    }

    // 🔥 修改讀取格式：解析 10 個欄位
    private void loadAlarmsFromPrefs() {
        Set<String> alarmSet = sharedPreferences.getStringSet("alarm_list_data", null);
        if (alarmSet != null) {
            for (String s : alarmSet) {
                String[] p = s.split("\\|");
                if (p.length >= 9) { // 兼容舊版資料
                    String desc = (p.length >= 10) ? p[9] : "";
                    alarmList.add(new AlarmItem(Integer.parseInt(p[0]), p[1], p[2], Boolean.parseBoolean(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                            Integer.parseInt(p[7]), Integer.parseInt(p[8]), desc));
                }
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}