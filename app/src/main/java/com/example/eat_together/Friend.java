package com.example.eat_together;

public class Friend {
    private String id;        // 使用者的 UUID
    private String name;
    private String email;
    private int imageResId;
    private boolean isFriend; // ★ 新增：是否已經是好友

    // 建構子
    public Friend(String id, String name, String email, int imageResId, boolean isFriend) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.imageResId = imageResId;
        this.isFriend = isFriend;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getImageResId() { return imageResId; }
    public boolean isFriend() { return isFriend; }

    public void setFriend(boolean friend) { isFriend = friend; }
}
