package com.example.eat_together;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // UI 元件
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private Button btnSend;

    // 資料變數
    private String myId;        // 我的 ID
    private String targetId;    // 對方 ID (如果是私聊) 或 群組 ID (如果是群聊)
    private String targetName;  // 對方名稱 或 群組名稱
    private String chatType;    // "PRIVATE" 或 "GROUP"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 初始化 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 2. 讀取 Intent 資料與使用者 ID
        Intent intent = getIntent();
        chatType = intent.getStringExtra("CHAT_TYPE");
        targetId = intent.getStringExtra("TARGET_ID");
        targetName = intent.getStringExtra("TARGET_NAME");

        // 預設值防呆
        if (chatType == null) chatType = "PRIVATE";
        if (targetName != null) getSupportActionBar().setTitle(targetName);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        myId = prefs.getString("user_id", "");

        // 3. 初始化 RecyclerView 與 Adapter
        recyclerView = findViewById(R.id.recycler_chat_content);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 4. 設定按鈕監聽器
        btnSend.setOnClickListener(v -> sendMessage());

        // 5. 設定 TCP 訊息監聽器 (核心部分)
        setupTcpListener();

        // 6. 背景執行：連線檢查與載入歷史訊息
        new Thread(this::checkConnectionAndLoadHistory).start();
    }

    // --- 選單功能 (群組邀請 & 建立活動) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 只有群組才顯示選單
        if ("GROUP".equals(chatType)) {
            getMenuInflater().inflate(R.menu.chat_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_invite) {
            showInviteDialog();
            return true;
        } else if (id == R.id.action_create_event) {
            // 跳轉到 MapActivity 建立聚餐
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("GROUP_ID", targetId);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- 核心邏輯區 ---

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        // 1. 先顯示在自己的畫面上
        addMessageToScreen("我", content, ChatMessage.TYPE_ME);
        etMessage.setText("");

        // 2. 背景發送給 Server
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (client.isConnected()) {
                String cmd;
                if ("GROUP".equals(chatType)) {
                    // 格式: GROUP_MSG:群組ID:內容
                    cmd = "GROUP_MSG:" + targetId + ":" + content;
                } else {
                    // 格式: MSG:好友ID:內容
                    cmd = "MSG:" + targetId + ":" + content;
                }
                client.sendMessage(cmd);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "連線中斷，請稍後再試", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupTcpListener() {
        TcpClient.getInstance().setListener(msg -> {
            // 收到訊息後，統一在 UI Thread 處理
            runOnUiThread(() -> handleReceivedMessage(msg));
        });
    }

    // 統一處理所有收到的訊息
    private void handleReceivedMessage(String msg) {
        if (msg == null) return;

        // 狀況 A: 私聊訊息 (NEW_MSG:ID:Name:Content)
        if (msg.startsWith("NEW_MSG:") && "PRIVATE".equals(chatType)) {
            String[] parts = msg.split(":", 4);
            if (parts.length == 4) {
                String senderId = parts[1];
                String senderName = parts[2];
                String content = parts[3];

                // 只有當傳訊者是目前的聊天對象才顯示
                if (senderId.equals(targetId)) {
                    addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER);
                }
            }
        }
        // 狀況 B: 群組訊息 (NEW_GROUP_MSG:GroupID:SenderID:SenderName:Content)
        else if (msg.startsWith("NEW_GROUP_MSG:") && "GROUP".equals(chatType)) {
            String[] parts = msg.split(":", 5);
            if (parts.length == 5) {
                String msgGroupId = parts[1];
                String senderId = parts[2];
                String senderName = parts[3];
                String content = parts[4];

                // 只有當訊息屬於目前群組，且不是我自己發的
                if (msgGroupId.equals(targetId) && !senderId.equals(myId)) {
                    addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER);
                }
            }
        }
        // 狀況 C: 歷史紀錄 JSON
        else if (msg.startsWith("HISTORY_JSON:")) {
            String jsonStr = msg.substring(13); // 去掉 prefix
            parseHistoryJson(jsonStr);
        }
        // 狀況 D: 收到活動/聚餐通知 (EVENT_MSG:ID:Title:Time...)
        else if (msg.startsWith("EVENT_MSG:")) {
            String[] parts = msg.split(":", 5);
            if (parts.length >= 5) {
                // String eventId = parts[2]; // 如果需要用到 eventId
                String title = parts[3];
                String time = parts[4];
                // 顯示一個特殊的活動訊息
                ChatMessage eventMsg = new ChatMessage("聚餐活動", "標題: " + title + "\n時間: " + time, ChatMessage.TYPE_OTHER);
                messageList.add(eventMsg);
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        }
        // 狀況 E: 邀請結果通知
        else if (msg.equals("INVITE_SUCCESS")) {
            Toast.makeText(this, "邀請成功！", Toast.LENGTH_SHORT).show();
        } else if (msg.startsWith("INVITE_FAIL:")) {
            Toast.makeText(this, "邀請失敗: " + msg.split(":")[1], Toast.LENGTH_SHORT).show();
        }
    }

    // --- 背景工作區 ---

    private void checkConnectionAndLoadHistory() {
        TcpClient client = TcpClient.getInstance();

        // 1. 如果斷線，嘗試重連
        if (!client.isConnected()) {
            client.connect();
        }

        // 2. 如果連線成功，補送 LOGIN 確保 Server 認識我
        if (client.isConnected()) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String email = prefs.getString("user_email", "");
            String pwd = prefs.getString("user_password", "");

            if (!email.isEmpty()) {
                client.sendMessage("LOGIN:" + email + ":" + pwd);
                // 稍微等待 Server 處理 Login (非必要但較穩)
                try { Thread.sleep(300); } catch (InterruptedException e) {}
            }

            // 3. 發送讀取歷史紀錄的指令
            loadHistoryCommand();
        }
    }

    private void loadHistoryCommand() {
        TcpClient client = TcpClient.getInstance();
        if ("GROUP".equals(chatType)) {
            client.sendMessage("GET_GROUP_HISTORY:" + targetId);
        } else {
            // 私聊歷史需要兩個人的 ID
            if (!myId.isEmpty()) {
                client.sendMessage("GET_CHAT_HISTORY:" + myId + ":" + targetId);
            }
        }
    }

    // --- JSON 解析區 ---

    private void parseHistoryJson(String jsonStr) {
        try {
            JSONArray array = new JSONArray(jsonStr);
            List<ChatMessage> historyList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                String content = obj.getString("content");
                String senderId = obj.getString("sender_id");
                // String time = obj.optString("created_at", ""); // 如果需要時間

                int type;
                String displayName;

                if (senderId.equals(myId)) {
                    type = ChatMessage.TYPE_ME;
                    displayName = "我";
                } else {
                    type = ChatMessage.TYPE_OTHER;
                    displayName = "對方"; // 預設

                    // 如果是群組，嘗試從 users 物件抓名字
                    if (!obj.isNull("users")) {
                        JSONObject userObj = obj.getJSONObject("users");
                        displayName = userObj.optString("username", "成員");
                    } else if ("PRIVATE".equals(chatType)) {
                        displayName = targetName; // 私聊直接用標題名字
                    }
                }

                ChatMessage msg = new ChatMessage(displayName, content, type);
                historyList.add(msg);
            }

            // 更新 UI
            messageList.clear();
            messageList.addAll(historyList);
            adapter.notifyDataSetChanged();
            if (!messageList.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ChatActivity", "JSON Parse Error: " + e.getMessage());
        }
    }

    // --- 輔助方法 ---

    private void addMessageToScreen(String name, String content, int type) {
        ChatMessage msg = new ChatMessage(name, content, type);
        messageList.add(msg);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void showInviteDialog() {
        final EditText input = new EditText(this);
        input.setHint("請輸入好友的 Email");

        new AlertDialog.Builder(this)
                .setTitle("邀請成員")
                .setMessage("請輸入對方的註冊 Email：")
                .setView(input)
                .setPositiveButton("邀請", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        // 發送邀請指令
                        new Thread(() -> {
                            TcpClient.getInstance().sendMessage("INVITE_MEMBER:" + targetId + ":" + email);
                        }).start();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}