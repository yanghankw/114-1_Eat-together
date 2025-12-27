package com.example.eat_together;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatsAdapter adapter;
    private List<ChatSession> chatList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // --- UI 美化設定 (保留您原本的) ---
        SearchView searchView = view.findViewById(R.id.sv_location);
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plateView = searchView.findViewById(plateId);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }

        recyclerView = view.findViewById(R.id.recycler_view_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. 初始化空列表 (先不放假資料)
        chatList = new ArrayList<>();
        adapter = new ChatsAdapter(getContext(), chatList);
        recyclerView.setAdapter(adapter);

        // 2. 讀取我的 User ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String myId = prefs.getString("user_id", null);

        if (myId != null) {
            // 3. 開始從 Server 抓好友
            loadFriendsFromServer(myId);
        } else {
            Toast.makeText(getContext(), "請先登入", Toast.LENGTH_SHORT).show();
        }

        // ★★★ 新增：綁定按鈕並設定跳轉 ★★★
        android.widget.Button btnTest = view.findViewById(R.id.btn_test_group);
        btnTest.setOnClickListener(v -> {
            // 準備跳轉到 ChatActivity
            android.content.Intent intent = new android.content.Intent(getContext(), ChatActivity.class);

            // 設定參數告訴 ChatActivity 這是「群組聊天」
            intent.putExtra("CHAT_TYPE", "GROUP");   // 告訴它是群組
            intent.putExtra("FRIEND_ID", "1");       // ★ 群組 ID (請確認資料庫 groups 表有 id=1 的資料)
            intent.putExtra("FRIEND_NAME", "美食團"); // 群組名稱 (顯示在標題)

            startActivity(intent);
        });

        return view;
    }

    private void loadFriendsFromServer(String myId) {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (!client.isConnected()) client.connect();

            // 發送指令: GET_FRIENDS:我的ID
            String response = client.sendRequest("GET_FRIENDS:" + myId);

            if (response != null && response.startsWith("FRIENDS_JSON:")) {
                // 去掉前綴，取出純 JSON 字串
                String jsonStr = response.substring("FRIENDS_JSON:".length());
                updateListWithJson(jsonStr);
            }
        }).start();
    }

    private void updateListWithJson(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<ChatSession> newChats = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                // 從 View (friend_details) 裡面的欄位抓資料
                // 注意：這裡我們暫時把 friend_id 忽略，但在做「點擊聊天」時會需要它
                // 建議您的 ChatSession 類別之後要加一個 id 欄位
                String name = obj.optString("friend_name", "未知好友");
                String email = obj.optString("friend_email", "");
                String friendId = obj.optString("friend_id");

                // 這裡暫時用 Email 當作「最後訊息」顯示，時間先顯示 "剛剛"
                newChats.add(new ChatSession(friendId, name, "想吃什麼？", "剛剛", R.drawable.ic_person));
            }

            // 回到主執行緒更新畫面
            getActivity().runOnUiThread(() -> {
                chatList.clear();
                chatList.addAll(newChats);
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}