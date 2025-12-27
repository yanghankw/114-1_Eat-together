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
            }
        }).start();
    }

    // ... (其餘方法保持不變) ...
    private void startListeningForMessages() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            while (isListening) {
                String msg = client.readMessage();
                if (msg != null && msg.startsWith("NEW_MSG:")) {
                    String[] parts = msg.split(":", 3);
                    if (parts.length == 3) {
                        String senderId = parts[1];
                        String content = parts[2];
                        if (senderId.equals(friendId)) {
                            runOnUiThread(() -> addMessageToScreen(content, ChatMessage.TYPE_OTHER));
                        }
                    }
                }
            }
        }).start();
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
}