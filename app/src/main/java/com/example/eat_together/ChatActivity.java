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
    private String myId;
    private String targetId;
    private String targetName;
    private String chatType;

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

        // 2. 讀取 Intent
        Intent intent = getIntent();
        chatType = intent.getStringExtra("CHAT_TYPE");
        targetId = intent.getStringExtra("TARGET_ID");
        targetName = intent.getStringExtra("TARGET_NAME");

        if (targetId == null) targetId = intent.getStringExtra("FRIEND_ID");
        if (targetName == null) targetName = intent.getStringExtra("FRIEND_NAME");

        if (chatType == null) chatType = "PRIVATE";
        if (targetName != null) getSupportActionBar().setTitle(targetName);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        myId = prefs.getString("user_id", "");

        // 3. 初始化 RecyclerView
        recyclerView = findViewById(R.id.recycler_chat_content);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList); // 確保你的 Adapter 是最新版 (支援卡片的)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 4. 按鈕監聽
        btnSend.setOnClickListener(v -> sendMessage());

        // 5. TCP 監聽
        setupTcpListener();

        // 6. 背景連線與載入歷史
        new Thread(this::checkConnectionAndLoadHistory).start();
    }

    // --- 選單功能 ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("GROUP_ID", targetId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- 核心邏輯 ---

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        // 顯示自己的文字訊息
        addMessageToScreen("我", content, ChatMessage.TYPE_ME);
        etMessage.setText("");

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (client.isConnected()) {
                String cmd;
                if ("GROUP".equals(chatType)) {
                    cmd = "GROUP_MSG:" + targetId + ":" + content;
                } else {
                    cmd = "MSG:" + targetId + ":" + content;
                }
                client.sendMessage(cmd);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "連線中斷", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupTcpListener() {
        TcpClient.getInstance().setListener(msg -> {
            runOnUiThread(() -> handleReceivedMessage(msg));
        });
    }

    private void handleReceivedMessage(String msg) {
        if (msg == null) return;

        // A. 私聊
        if (msg.startsWith("NEW_MSG:") && "PRIVATE".equals(chatType)) {
            String[] parts = msg.split(":", 4);
            if (parts.length == 4) {
                String senderId = parts[1];
                String senderName = parts[2];
                String content = parts[3];
                if (senderId.equals(targetId)) {
                    addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER);
                }
            }
        }
        // B. 群組
        else if (msg.startsWith("NEW_GROUP_MSG:") && "GROUP".equals(chatType)) {
            String[] parts = msg.split(":", 5);
            if (parts.length == 5) {
                String msgGroupId = parts[1];
                String senderId = parts[2];
                String senderName = parts[3];
                String content = parts[4];
                if (msgGroupId.equals(targetId) && !senderId.equals(myId)) {
                    addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER);
                }
            }
        }
        // C. 歷史紀錄
        else if (msg.startsWith("HISTORY_JSON:")) {
            String jsonStr = msg.substring(13);
            parseHistoryJson(jsonStr);
        }
        // D. ★★★ 修正這裡：收到活動通知時 ★★★
        else if (msg.startsWith("EVENT_MSG:")) {
            // 格式: EVENT_MSG:GroupID:EventID:Title:Time
            String[] parts = msg.split(":", 5);
            if (parts.length >= 5) {
                String eventId = parts[2];
                String title = parts[3];
                String time = parts[4];

                // 組合給 Adapter 解析的字串 (ID,標題,時間)
                String adapterContent = eventId + "," + title + "," + time;

                // ★★★ 關鍵：使用 TYPE_EVENT (2)，不要用 TYPE_OTHER ★★★
                addMessageToScreen("系統通知", adapterContent, ChatMessage.TYPE_EVENT);
            }
        }
        else if (msg.equals("INVITE_SUCCESS")) {
            Toast.makeText(this, "邀請成功！", Toast.LENGTH_SHORT).show();
        } else if (msg.startsWith("INVITE_FAIL:")) {
            Toast.makeText(this, "邀請失敗: " + msg.split(":")[1], Toast.LENGTH_SHORT).show();
        }
    }

    // --- 背景工作 ---
    private void checkConnectionAndLoadHistory() {
        TcpClient client = TcpClient.getInstance();
        if (!client.isConnected()) client.connect();

        if (client.isConnected()) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String email = prefs.getString("user_email", "");
            String pwd = prefs.getString("user_password", "");
            if (!email.isEmpty()) {
                client.sendMessage("LOGIN:" + email + ":" + pwd);
                try { Thread.sleep(300); } catch (InterruptedException e) {}
            }
            loadHistoryCommand();
        }
    }

    private void loadHistoryCommand() {
        TcpClient client = TcpClient.getInstance();
        if ("GROUP".equals(chatType)) {
            client.sendMessage("GET_GROUP_HISTORY:" + targetId);
        } else {
            if (!myId.isEmpty()) {
                client.sendMessage("GET_CHAT_HISTORY:" + myId + ":" + targetId);
            }
        }
    }

    // --- JSON 解析 (載入歷史) ---
    private void parseHistoryJson(String jsonStr) {
        try {
            JSONArray array = new JSONArray(jsonStr);
            List<ChatMessage> historyList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                String content = obj.getString("content");
                String senderId = obj.getString("sender_id");

                // ★★★ 新增：判斷是否為活動訊息 ★★★
                // 這裡是一個簡單的判斷：如果內容包含逗號且看起來像活動格式
                // 或者你的資料庫有存 message_type 欄位，最好用那个判斷
                boolean isEvent = false;

                // 假設你的資料庫存活動內容是 "ID,地點,時間"
                // 這裡我們簡單檢查：如果 sender_id 是 "SYSTEM" 或者是特殊格式，就當作活動
                // 如果你之前是自己發送的，可以用內容格式判斷 (例如包含 "2025-" 和 ",")
                if (content.matches("\\d+,.*,.*")) { // 簡單正則：數字,文字,文字
                    isEvent = true;
                }

                // 如果你的後端有回傳 message_type，請用這行：
                // if ("event".equals(obj.optString("message_type"))) isEvent = true;

                int type;
                String senderName;

                if (isEvent) {
                    type = ChatMessage.TYPE_EVENT; // ★ 設定為活動卡片
                    senderName = "系統通知";
                } else if (senderId.equals(myId)) {
                    type = ChatMessage.TYPE_ME;
                    senderName = "我";
                } else {
                    type = ChatMessage.TYPE_OTHER;
                    if (!obj.isNull("users")) {
                        JSONObject userObj = obj.getJSONObject("users");
                        senderName = userObj.optString("username", "對方");
                    } else {
                        senderName = targetName != null ? targetName : "對方";
                    }
                }

                // 建立訊息物件
                ChatMessage msg = new ChatMessage(content, type, senderName);
                historyList.add(msg);
            }

            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(historyList);
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ChatActivity", "JSON Parse Error: " + e.getMessage());
        }
    }

    // --- 輔助方法 ---
    private void addMessageToScreen(String senderName, String content, int type) {
        ChatMessage msg = new ChatMessage(content, type, senderName);
        messageList.add(msg);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void showInviteDialog() {
        final EditText input = new EditText(this);
        input.setHint("請輸入好友的 Email");
        new AlertDialog.Builder(this)
                .setTitle("邀請成員")
                .setView(input)
                .setPositiveButton("邀請", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        new Thread(() -> TcpClient.getInstance().sendMessage("INVITE_MEMBER:" + targetId + ":" + email)).start();
                    }
                })
                .setNegativeButton("取消", null).show();
    }
}