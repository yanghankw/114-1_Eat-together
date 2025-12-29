package com.example.eat_together;

public class AlarmItem {
    public int id;
    public String time;
    public String date;
    public boolean isOn;
    public int year, month, day, hour, minute;
    public String description; // 🔥 新增描述欄位

    public AlarmItem(int id, String time, String date, boolean isOn, int year, int month, int day, int hour, int minute, String description) {
        this.id = id;
        this.time = time;
        this.date = date;
        this.isOn = isOn;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.description = description;
    }
}