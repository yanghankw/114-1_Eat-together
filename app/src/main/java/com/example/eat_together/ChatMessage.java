package com.example.eat_together;

public class ChatMessage {
    public static final int TYPE_ME = 0;
    public static final int TYPE_OTHER = 1;
    // ★ 新增：活動類型
    public static final int TYPE_EVENT = 2;

    private String senderName; // ★ 新增：發送者名字
    private String content;
    // ★ 新增：活動相關欄位
    private String eventId;
    private String eventTitle;
    private String eventTime;
    private int type;

    // ★ 修改建構子：多接收一個 senderName
    public ChatMessage(String senderName, String content, int type) {
        this.senderName = senderName;
        this.content = content;
        this.type = type;
    }

    // ★ 新增：專門給活動用的建構子
    public ChatMessage(String eventId, String title, String time) {
        this.type = TYPE_EVENT;
        this.eventId = eventId;
        this.eventTitle = title;
        this.eventTime = time;
        this.content = "[聚餐活動]"; // 預設內容，避免空指標
        this.senderName = "系統通知";
    }

    // ★ 新增：Getter 方法
    public String getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getEventTime() { return eventTime; }

    public String getSenderName() { return senderName; } // ★ 新增 Getter
    public String getContent() { return content; }
    public int getType() { return type; }
}