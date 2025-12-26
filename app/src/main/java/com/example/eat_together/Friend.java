package com.example.eat_together;

public class Friend {
    private String id; // ★ 1. 必須有這個變數
    private String name;
    private String email;
    private int imageResId;

    // ★ 2. 建構子必須接收 id
    public Friend(String id, String name, String email, int imageResId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.imageResId = imageResId;
    }

    // ★ 3. 必須有 getter
    public String getId() {
        return id;
    }

    public String getName() { return name; }
    public String getEmail() { return email; } // 原本的 status 欄位改用 email 顯示
    public int getImageResId() { return imageResId; }
}