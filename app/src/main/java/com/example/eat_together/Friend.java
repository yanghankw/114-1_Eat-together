package com.example.eat_together;

public class Friend {
    private String name;
    private String status;
    // 這裡為了簡單先用 int 代表圖片資源 ID，未來連線後可改成 String (URL)
    private int avatarResId;

    public Friend(String name, String status, int avatarResId) {
        this.name = name;
        this.status = status;
        this.avatarResId = avatarResId;
    }

    public String getName() { return name; }
    public String getStatus() { return status; }
    public int getAvatarResId() { return avatarResId; }
}
