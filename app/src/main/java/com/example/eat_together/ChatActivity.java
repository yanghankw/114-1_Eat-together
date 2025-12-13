package com.example.eat_together;

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

    // 🔥 新增變數：用來存現在聊天的房間名稱 (Server 需要知道你是誰)
    private String currentChatName = "DefaultRoom"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 取得傳過來的名字並設定標題
        String chatName = getIntent().getStringExtra("CHAT_NAME");
        if (chatName != null) {
            setTitle(chatName);
            // 🔥 新增：如果有傳房間名過來，就記下來
            currentChatName = chatName; 
        }

        // 接收地圖傳來的資料
        String placeName = getIntent().getStringExtra("PLACE_NAME");
        String placeAddress = getIntent().getStringExtra("PLACE_ADDRESS");

        // 你的 ID 綁定 (完全沒動)
        recyclerView = findViewById(R.id.recycler_chat_content);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        // 你的假資料 (完全沒動)
        messageList.add(new ChatMessage("嗨！要吃午餐嗎？", ChatMessage.TYPE_OTHER));
        messageList.add(new ChatMessage("好啊，去哪吃？", ChatMessage.TYPE_ME));

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 你的自動填入文字邏輯 (完全沒動)
        if (placeName != null) {
            String messageDraft = "我們去吃這家吧！\n" + placeName + "\n地址：" + placeAddress;
            etMessage.setText(messageDraft); 
        }

        // 發送按鈕邏輯
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString();
            if (!content.isEmpty()) {
                // 1. 原本的功能：更新手機畫面 (完全沒動)
                messageList.add(new ChatMessage(content, ChatMessage.TYPE_ME));
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
                
                // ==================================================
                // 🔥 唯一新增的地方：告訴 Server 你說了什麼
                // ==================================================
                // 拼湊指令格式 -> MSG:房間名:內容
                String tcpMessage = "MSG:" + currentChatName + ":" + content;
                
                // 呼叫 TcpClient 送出去
                // (請確保你有建立 TcpClient.java 這個檔案)
                TcpClient.getInstance().sendMessage(tcpMessage);
                // ==================================================

                // 清空輸入框
                etMessage.setText("");
            }
        });
    }
}