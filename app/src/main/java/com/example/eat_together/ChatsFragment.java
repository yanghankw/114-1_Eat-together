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
            loadAllChats(myId);
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

    private void loadAllChats(String myId) {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (!client.isConnected()) client.connect();

            // 1. 先清空目前的列表 (避免重複)
            getActivity().runOnUiThread(() -> {
                chatList.clear();
                adapter.notifyDataSetChanged();
            });

            // ---------------------------------------------
            // A. 抓好友 (PRIVATE)
            // ---------------------------------------------
            String friendResponse = client.sendRequest("GET_FRIENDS:" + myId);
            if (friendResponse != null && friendResponse.startsWith("FRIENDS_JSON:")) {
                String jsonStr = friendResponse.substring("FRIENDS_JSON:".length());
                // 解析並加入列表 (type = "PRIVATE")
                parseAndAddChats(jsonStr, "PRIVATE");
            }

            // ---------------------------------------------
            // B. 抓群組 (GROUP)
            // ---------------------------------------------
            String groupResponse = client.sendRequest("GET_MY_GROUPS:" + myId); // 記得 Server 要實作這個
            if (groupResponse != null && groupResponse.startsWith("GROUPS_JSON:")) {
                String jsonStr = groupResponse.substring("GROUPS_JSON:".length());
                // 解析並加入列表 (type = "GROUP")
                parseAndAddChats(jsonStr, "GROUP");
            }

        }).start();
    }

    // 通用的解析方法
    private void parseAndAddChats(String jsonString, String type) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<ChatSession> tempAddList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String id = "";
                String name = "";

                if ("PRIVATE".equals(type)) {
                    // 解析好友 JSON 結構
                    name = obj.optString("friend_name", "未知好友");
                    id = obj.optString("friend_id");
                } else {
                    // 解析群組 JSON 結構
                    // Supabase 回傳長這樣: [{"group_id": 1, "groups": {"name": "美食團"}}]
                    id = String.valueOf(obj.optInt("group_id")); // 群組 ID 是數字

                    JSONObject groupObj = obj.optJSONObject("groups");
                    if (groupObj != null) {
                        name = groupObj.optString("name", "未命名群組");
                    } else {
                        name = "群組 " + id;
                    }
                }

                // 加入列表 (icon 可以根據 type 換不同的圖)
                int iconRes = "GROUP".equals(type) ? R.drawable.ic_launcher_foreground : R.drawable.ic_person; // 暫時用內建圖

                // ★ 這裡傳入 type
                tempAddList.add(new ChatSession(id, name, "點擊查看訊息", "剛剛", iconRes, type));
            }

            // 更新 UI (使用 addAll 累加資料)
            getActivity().runOnUiThread(() -> {
                chatList.addAll(tempAddList);
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}