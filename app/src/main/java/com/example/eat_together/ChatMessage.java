package com.example.eat_together;

public class ChatMessage {
    // 定義訊息類型常數
    public static final int TYPE_ME = 0;
    public static final int TYPE_OTHER = 1;
    public static final int TYPE_EVENT = 2; // ★ 新增：活動卡片類型

    private String name;
    private String content; // 如果是文字訊息，存內容；如果是活動，這欄位可以存 ID 或留空
    private int type;
    private String time;

    // ★ 新增：活動專用欄位
    private String eventTitle;
    private String eventTime;
    private String eventId;

    // 一般文字訊息的建構子
    public ChatMessage(String name, String content, int type) {
        this.name = name;
        this.content = content;
        this.type = type;
    }

    // ★ 新增：活動訊息的建構子
    public ChatMessage(String eventId, String title, String time) {
        this.type = TYPE_EVENT;
        this.eventId = eventId;
        this.eventTitle = title;
        this.eventTime = time;
        this.name = "系統通知"; // 活動通常顯示為系統通知
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getContent() { return content; }
    public int getType() { return type; }
    public void setSenderName(String name) { this.name = name; }
    public void setTime(String time) { this.time = time; }

    public String getEventTitle() { return eventTitle; }
    public String getEventTime() { return eventTime; }
    public String getEventId() { return eventId; }
}