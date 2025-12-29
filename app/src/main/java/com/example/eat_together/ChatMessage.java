package com.example.eat_together;

public class ChatMessage {

    // --- 定義訊息類型常數 ---
    // 0 = 自己發的 (右邊)
    public static final int TYPE_ME = 0;

    // 1 = 對方發的 (左邊)
    public static final int TYPE_OTHER = 1;

    // 2 = 活動邀請卡片 (置中顯示黃色卡片)
    public static final int TYPE_EVENT = 2;

    // --- 資料變數 ---
    private String content;     // 訊息內容 (如果是活動，這裡會存 "ID,地點,時間")
    private int type;           // 訊息類型 (0, 1, 或 2)
    private String senderName;  // 發送者名字 (用來產生 RoboHash 頭像)

    // --- 建構子 (Constructor) ---
    // 注意參數順序：內容 -> 類型 -> 名字
    public ChatMessage(String content, int type, String senderName) {
        this.content = content;
        this.type = type;
        this.senderName = senderName;
    }

    // --- Getter 方法 (讓 Adapter 讀取資料) ---

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public String getSenderName() {
        return senderName;
    }
}