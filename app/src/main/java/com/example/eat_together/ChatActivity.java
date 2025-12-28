package com.example.eat_together;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private String chatType; // "PRIVATE" or "GROUP"
    private String targetId; // 如果是私聊就是 friendId，如果是群聊就是 groupId

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 只有是群組 ("GROUP") 才顯示邀請按鈕
        if ("GROUP".equals(chatType)) {
            getMenuInflater().inflate(R.menu.chat_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    // ★ 2. 處理選單點擊事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_invite) {
            showInviteDialog(); // 既有的邀請功能
            return true;
        }
        // ★ 新增這段：跳轉到地圖頁面建立活動
        else if (id == R.id.action_create_event) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("GROUP_ID", targetId); // 把群組 ID 帶過去
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ★★★ 加入這幾行來設定 Toolbar ★★★
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // 這句話是關鍵！啟動標題列功能

        // 設定標題
        if (getSupportActionBar() != null) {
            if (friendName != null) {
                getSupportActionBar().setTitle(friendName);
            }
            // 顯示返回鍵 (選用)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 1. 接收 Intent 參數
        // 預設是 PRIVATE (為了相容舊程式碼)
        chatType = getIntent().getStringExtra("CHAT_TYPE");
        android.util.Log.d("ActivityDebug", "收到 CHAT_TYPE: " + chatType); // ★ 加這行 Log
        if (chatType == null) chatType = "PRIVATE";

        // 把原本的 friendId 改名為 targetId 會比較通用，但為了少改code，我們先這樣用：
        // 如果是群組，FRIEND_ID 傳的就是 GroupID
        targetId = getIntent().getStringExtra("FRIEND_ID");
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
                addMessageToScreen("我", content, ChatMessage.TYPE_ME);

                String tcpMessage = "MSG:" + friendId + ":" + content;
                android.util.Log.d("ChatDebug", "準備發送: " + tcpMessage);

                new Thread(() -> {
                    TcpClient client = TcpClient.getInstance();
                    if (client.isConnected()) {
                        String cmd;
                        if ("GROUP".equals(chatType)) {
                            // ★ 群組發送格式: GROUP_MSG:群組ID:內容
                            cmd = "GROUP_MSG:" + targetId + ":" + content;
                        } else {
                            // ★ 私聊發送格式: MSG:好友ID:內容
                            cmd = "MSG:" + targetId + ":" + content;
                        }
                        client.sendMessage(cmd);
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

    // ★ 1. 建立右上角選單


    // ★ 3. 顯示輸入 Email 的對話框
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
                    sendInviteCommand(email);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ★ 4. 發送 TCP 指令給 Server
    private void sendInviteCommand(String targetEmail) {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            // 格式: INVITE_MEMBER:群組ID:Email
            String cmd = "INVITE_MEMBER:" + targetId + ":" + targetEmail;
            client.sendMessage(cmd);
        }).start();
    }

    private void startListeningForMessages() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            while (isListening) {
                String msg = client.readMessage();
                if (msg != null) {

                    // A. 私聊訊息 (格式更新: NEW_MSG:ID:Name:Content)
                    if (msg.startsWith("NEW_MSG:") && "PRIVATE".equals(chatType)) {
                        // 切成 4 段
                        String[] parts = msg.split(":", 4);
                        if (parts.length == 4) {
                            String senderId = parts[1];
                            String senderName = parts[2]; // ★ 抓出名字
                            String content = parts[3];    // ★ 這就是您原本缺少的 content 變數

                            if (senderId.equals(targetId)) {
                                runOnUiThread(() -> addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER));
                            }
                        }
                    }

                    // B. 群組訊息 (格式更新: NEW_GROUP_MSG:GroupID:SenderID:SenderName:Content)
                    else if (msg.startsWith("NEW_GROUP_MSG:") && "GROUP".equals(chatType)) {
                        // 切成 5 段
                        String[] parts = msg.split(":", 5);
                        if (parts.length == 5) {
                            String msgGroupId = parts[1];
                            String senderId = parts[2];
                            String senderName = parts[3]; // ★ 抓出名字
                            String content = parts[4];

                            // 只有當訊息屬於目前這個群組，且不是我自己發的
                            if (msgGroupId.equals(targetId) && !senderId.equals(myId)) {
                                runOnUiThread(() -> addMessageToScreen(senderName, content, ChatMessage.TYPE_OTHER));
                            }
                        }
                    }

                    // C. 歷史訊息
                    else if (msg.startsWith("HISTORY_JSON:")) {
                        String jsonStr = msg.substring("HISTORY_JSON:".length());
                        parseHistoryJson(jsonStr);
                    }

                    else if (msg.equals("INVITE_SUCCESS")) {
                        runOnUiThread(() ->
                                Toast.makeText(ChatActivity.this, "邀請成功！", Toast.LENGTH_SHORT).show()
                        );
                    }
                    else if (msg.startsWith("INVITE_FAIL:")) {
                        String reason = msg.split(":")[1];
                        runOnUiThread(() ->
                                Toast.makeText(ChatActivity.this, "失敗: " + reason, Toast.LENGTH_SHORT).show()
                        );
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

                int type;
                String displayName = "成員"; // 預設值

                if (senderId.equals(myId)) {
                    type = ChatMessage.TYPE_ME;
                    displayName = "我";
                } else {
                    type = ChatMessage.TYPE_OTHER;

                    if ("GROUP".equals(chatType)) {
                        // ★★★ 修改重點：從巢狀的 users 物件裡抓 username ★★★
                        // 檢查有沒有 "users" 這個欄位
                        if (!obj.isNull("users")) {
                            org.json.JSONObject userObj = obj.getJSONObject("users");
                            displayName = userObj.optString("username", "成員");
                        }
                    } else {
                        // 私聊的話，對方名字通常就是好友名字 (或是您之後也可以用同樣的 Join 技巧來查)
                        displayName = friendName != null ? friendName : "對方";
                    }
                }

                historyList.add(new ChatMessage(displayName, content, type));
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
            android.util.Log.e("ChatDebug", "解析歷史訊息失敗: " + e.getMessage());
        }
    }

    private void addMessageToScreen(String name, String content, int type) {
        // ★ 傳入名字
        messageList.add(new ChatMessage(name, content, type));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadHistory() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            // ★ 修正 1: 改用 targetId 判斷，不要用 friendId (它是 null)
            if (myId == null || targetId == null) return;

            String cmd;

            // ★ 修正 2: 根據聊天類型，發送不同的查詢指令
            if ("GROUP".equals(chatType)) {
                // 群組歷史: GET_GROUP_HISTORY:群組ID
                cmd = "GET_GROUP_HISTORY:" + targetId;
            } else {
                // 私聊歷史: GET_CHAT_HISTORY:我的ID:對方ID
                cmd = "GET_CHAT_HISTORY:" + myId + ":" + targetId;
            }

            android.util.Log.d("ChatDebug", "正在索取歷史紀錄: " + cmd);
            client.sendMessage(cmd);

        }).start();
    }
}
