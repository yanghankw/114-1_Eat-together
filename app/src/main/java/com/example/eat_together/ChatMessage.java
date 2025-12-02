package com.example.eat_together;

public class ChatMessage {
    public static final int TYPE_ME = 0;
    public static final int TYPE_OTHER = 1;

    private String content;
    private int type; // 用來區分是自己(0)還是對方(1)

    public ChatMessage(String content, int type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() { return content; }
    public int getType() { return type; }
}
