package com.example.eat_together;

public class ChatMessage {
    public static final int TYPE_ME = 0;
    public static final int TYPE_OTHER = 1;

    private String senderName; // ★ 新增：發送者名字
    private String content;
    private int type;

    // ★ 修改建構子：多接收一個 senderName
    public ChatMessage(String senderName, String content, int type) {
        this.senderName = senderName;
        this.content = content;
        this.type = type;
    }

    public String getSenderName() { return senderName; } // ★ 新增 Getter
    public String getContent() { return content; }
    public int getType() { return type; }
}