package com.example.eat_together;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private Button btnSend;

    private String friendId;
    private String friendName;
    private String myId;

    private boolean isListening = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 獲取資料
        friendId = getIntent().getStringExtra("FRIEND_ID");
        friendName = getIntent().getStringExtra("FRIEND_NAME");

        // --- 第一次宣告 prefs (這是正確的) ---
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        myId = prefs.getString("user_id", null);

        if (friendName != null) setTitle(friendName);

        // 2. 初始化 UI
        recyclerView = findViewById(R.id.recycler_chat_content);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 3. 按鈕發送邏輯
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString();

            if (!content.isEmpty()) {
                // ★ 記得要把這行加回來，不然您發送的訊息自己看不到
                addMessageToScreen(content, ChatMessage.TYPE_ME);

                String tcpMessage = "MSG:" + friendId + ":" + content;
                android.util.Log.d("ChatDebug", "準備發送: " + tcpMessage);

                new Thread(() -> {
                    TcpClient client = TcpClient.getInstance();
                    if (client.isConnected()) {
                        client.sendMessage(tcpMessage);
                        android.util.Log.d("ChatDebug", "已呼叫 sendMessage (送出成功)");
                    } else {
                        android.util.Log.e("ChatDebug", "發送失敗：TcpClient 未連線");
                        // 嘗試緊急連線
                        client.connect();
                        if(client.isConnected()) client.sendMessage(tcpMessage);
                    }
                }).start();

                etMessage.setText("");
            }
        });

        // 4. 啟動監聽
        startListeningForMessages();

        // 5. 自動連線與登入機制
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();

            // 1. 如果斷線了，先連線
            if (!client.isConnected()) {
                android.util.Log.d("ChatDebug", "發現未連線，嘗試重新連線...");
                client.connect();
            }

            // 2. ★ 關鍵修改：不管原本有沒有連線，只要現在是通的，就補送一次 LOGIN
            // 這樣可以修復「被其他頁面連線但未登入」的問題
            if (client.isConnected()) {
                // 使用 reloginPrefs 避免變數名稱衝突
                SharedPreferences reloginPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String savedEmail = reloginPrefs.getString("user_email", "");
                String savedPassword = reloginPrefs.getString("user_password", "");

                if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                    android.util.Log.d("ChatDebug", "正在補送登入指令 (確保 Server 認識我)...");
                    client.sendMessage("LOGIN:" + savedEmail + ":" + savedPassword);
                }

                // ★ 登入完畢後，載入歷史訊息！
                loadHistory();
            }
        }).start();
    }

    private void startListeningForMessages() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            while (isListening) {
                String msg = client.readMessage(); // 讀取一行

                if (msg != null) {

                    // A. 處理即時訊息 (原本的邏輯)
                    if (msg.startsWith("NEW_MSG:")) {
                        String[] parts = msg.split(":", 3);
                        if (parts.length == 3) {
                            String senderId = parts[1];
                            String content = parts[2];
                            if (senderId.equals(friendId)) {
                                runOnUiThread(() -> addMessageToScreen(content, ChatMessage.TYPE_OTHER));
                            }
                        }
                    }

                    // B. ★ 新增：處理歷史記錄回傳
                    else if (msg.startsWith("HISTORY_JSON:")) {
                        String jsonStr = msg.substring("HISTORY_JSON:".length());
                        parseHistoryJson(jsonStr);
                    }
                }
            }
        }).start();
    }

    // 解析歷史 JSON 並顯示
    private void parseHistoryJson(String jsonStr) {
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            List<ChatMessage> historyList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);

                String senderId = obj.getString("sender_id");
                String content = obj.getString("content");

                // 判斷這句話是「我講的」還是「對方講的」
                int type;
                if (senderId.equals(myId)) {
                    type = ChatMessage.TYPE_ME;
                } else {
                    type = ChatMessage.TYPE_OTHER;
                }

                historyList.add(new ChatMessage(content, type));
            }

            // 更新 UI
            runOnUiThread(() -> {
                // 把歷史訊息加在最前面，或是清空再加
                messageList.clear(); // 先清空，避免重複
                messageList.addAll(historyList);
                adapter.notifyDataSetChanged();
                // 捲動到最底部
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMessageToScreen(String content, int type) {
        messageList.add(new ChatMessage(content, type));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
    }

    private void loadHistory() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (myId == null || friendId == null) return;

            // 1. 發送指令
            // 格式: GET_CHAT_HISTORY:我的ID:對方ID
            String cmd = "GET_CHAT_HISTORY:" + myId + ":" + friendId;
            // 這裡我們直接用 sendMessage，然後靠監聽迴圈來收 HISTORY_JSON
            // (或是您可以用 client.sendRequest 等待回應，這裡示範用監聽接收)
            client.sendMessage(cmd);

        }).start();
    }
}
