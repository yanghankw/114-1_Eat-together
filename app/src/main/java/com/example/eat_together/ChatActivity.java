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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 取得傳過來的名字並設定標題
        String chatName = getIntent().getStringExtra("CHAT_NAME");
        if (chatName != null) setTitle(chatName);

        // ▼▼▼ 新增：接收地圖傳來的資料 ▼▼▼
        String placeName = getIntent().getStringExtra("PLACE_NAME");
        String placeAddress = getIntent().getStringExtra("PLACE_ADDRESS");

        recyclerView = findViewById(R.id.recycler_chat_content);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        // 假資料
        messageList.add(new ChatMessage("嗨！要吃午餐嗎？", ChatMessage.TYPE_OTHER));
        messageList.add(new ChatMessage("好啊，去哪吃？", ChatMessage.TYPE_ME));

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 如果地點資料存在，代表是從地圖跳轉過來的
        if (placeName != null) {
            String messageDraft = "我們去吃這家吧！\n" + placeName + "\n地址：" + placeAddress;
            etMessage.setText(messageDraft); // 自動填入輸入框
        }

        // 發送按鈕邏輯
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString();
            if (!content.isEmpty()) {
                // 新增一則「我」的訊息
                messageList.add(new ChatMessage(content, ChatMessage.TYPE_ME));
                // 通知 Adapter 更新
                adapter.notifyItemInserted(messageList.size() - 1);
                // 捲動到底部
                recyclerView.scrollToPosition(messageList.size() - 1);
                // 清空輸入框
                etMessage.setText("");
            }
        });
    }
}