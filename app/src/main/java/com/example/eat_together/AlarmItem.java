package com.example.eat_together;

public class AlarmItem {
    public int id;
    public String time;
    public String date;
    public boolean isOn;
    // 🔥 新增原始數值欄位，用來重新啟動鬧鐘
    public int year, month, day, hour, minute;

    public AlarmItem(int id, String time, String date, boolean isOn, int year, int month, int day, int hour, int minute) {
        this.id = id;
        this.time = time;
        this.date = date;
        this.isOn = isOn;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }
}