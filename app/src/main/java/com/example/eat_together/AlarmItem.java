package com.example.eat_together;

public class AlarmItem {
    public int id;
    public String time;
    public String date;
    public boolean isOn;

    public AlarmItem(int id, String time, String date, boolean isOn) {
        this.id = id;
        this.time = time;
        this.date = date;
        this.isOn = isOn;
    }
}