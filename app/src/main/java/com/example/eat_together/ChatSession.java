package com.example.eat_together;

public class ChatSession {
    private String friendId; // ★ 新增：好友的 UUID
    private String name;
    private String lastMessage;
    private String time;
    private int avatarResId;

    // ★ 修改建構子：加入 friendId
    public ChatSession(String friendId, String name, String lastMessage, String time, int avatarResId) {
        this.friendId = friendId; // 記得存起來
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
    }

    // ★ 新增 Getter
    public String getFriendId() { return friendId; }

    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getAvatarResId() { return avatarResId; }
}