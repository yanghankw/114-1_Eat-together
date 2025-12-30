package com.example.eat_together;

public class ChatSession {
    private String id; // 好友ID 或 群組ID
    private String name;
    private String lastMessage;
    private String time;
    private int avatarResId;
    private String type; // ★ 新增：用來分辨 "PRIVATE" 或 "GROUP"

    // ★ 修改建構子：加入 type
    public ChatSession(String id, String name, String lastMessage, String time, int avatarResId, String type) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
        this.type = type;
    }

    // Getters
    public String getId() { return id; } // 建議把原本的 getFriendId 改名為 getId 比較通用，或直接加這個方法
    public String getFriendId() { return id; } // 保留舊的相容
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getAvatarResId() { return avatarResId; }
    public String getType() { return type; } // ★ 新增 Getter
}