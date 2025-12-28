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
        android.widget.Button btnCreateGroup = view.findViewById(R.id.btn_test_group); // XML 裡的 ID
        btnCreateGroup.setOnClickListener(v -> {
            showCreateGroupDialog();
        });

        return view;
    }

    // ★ 新增：顯示建立群組的對話框
    private void showCreateGroupDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("建立新群組");

        // 設定輸入框
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("請輸入群組名稱");
        builder.setView(input);

        // 設定「建立」按鈕
        builder.setPositiveButton("建立", (dialog, which) -> {
            String groupName = input.getText().toString();
            if (!groupName.isEmpty()) {
                createGroupOnServer(groupName);
            }
        });

        // 設定「取消」按鈕
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // ★ 新增：發送建立指令給 Server
    // ChatsFragment.java

    private void createGroupOnServer(String groupName) {
        // 讀取我的 ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String myId = prefs.getString("user_id", null);

        if (myId == null) {
            Toast.makeText(getContext(), "無法識別身分，請重新登入", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (client.isConnected()) {
                // ★ 修改 1: 指令加入 myId (格式: CREATE_GROUP:群組名:ID)
                String response = client.sendRequest("CREATE_GROUP:" + groupName + ":" + myId);

                // ★ 修改 2: 增加安全檢查，防止切換頁面後崩潰
                if (getActivity() == null) return;

                if (response != null && response.startsWith("CREATE_GROUP_SUCCESS:")) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "群組建立成功！", Toast.LENGTH_SHORT).show();
                        loadAllChats(myId);
                    });
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "建立失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
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
                String lastMsg = "點擊開始聊天"; // 預設值
                String time = "";

                // 修改私聊 (PRIVATE) 的解析邏輯
                if ("PRIVATE".equals(type)) {
                    // 讀取新的欄位 (對應 SQL View)
                    name = obj.optString("friend_name", "未知好友");
                    id = obj.optString("friend_id");

                    // 讀取最後訊息
                    if (!obj.isNull("last_msg")) {
                        lastMsg = obj.getString("last_msg");
                    }

                    // 讀取時間
                    if (!obj.isNull("last_time")) {
                        String rawTime = obj.getString("last_time");
                        time = formatDisplayTime(rawTime); // 直接轉換成「今天」、「昨天」或「日期」
                    }

                } else { // GROUP
                    // ★★★ 群組邏輯修改 ★★★
                    // 因為我們改了 View，現在 JSON 結構變扁平了，直接在 obj 這一層
                    id = String.valueOf(obj.optInt("group_id"));
                    name = obj.optString("group_name", "群組 " + id);

                    // ★ 讀取最後訊息 (如果沒有就顯示預設)
                    if (!obj.isNull("last_msg")) {
                        lastMsg = obj.getString("last_msg");
                    }

                    // ★ 讀取時間
                    if (!obj.isNull("last_time")) {
                        String rawTime = obj.getString("last_time");
                        time = formatDisplayTime(rawTime); // 直接轉換成「今天」、「昨天」或「日期」
                    }
                }

                // 加入列表
                int iconRes = "GROUP".equals(type) ? R.drawable.ic_launcher_foreground : R.drawable.ic_person;

                // ★ 將 lastMsg 和 time 傳入
                tempAddList.add(new ChatSession(id, name, lastMsg, time, iconRes, type));
            }

            getActivity().runOnUiThread(() -> {
                chatList.addAll(tempAddList);
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ★★★ 新增：智慧時間格式化工具 ★★★
    private String formatDisplayTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) return "";

        try {
            // 1. 預處理字串：把 "T" 換成空白，並去除毫秒 (如果有小數點)
            // 輸入可能像: "2025-12-28T16:20:30.123" 或 "2025-12-28 16:20:30"
            String cleanTime = rawTime.replace("T", " ");
            if (cleanTime.contains(".")) {
                cleanTime = cleanTime.substring(0, cleanTime.indexOf("."));
            }

            // 2. 解析時間
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date msgDate = sdf.parse(cleanTime);
            java.util.Date now = new java.util.Date();

            // 3. 使用 Calendar 比較日期
            java.util.Calendar calMsg = java.util.Calendar.getInstance();
            calMsg.setTime(msgDate);

            java.util.Calendar calNow = java.util.Calendar.getInstance();
            calNow.setTime(now);

            boolean isSameYear = calMsg.get(java.util.Calendar.YEAR) == calNow.get(java.util.Calendar.YEAR);
            boolean isSameDay = isSameYear && (calMsg.get(java.util.Calendar.DAY_OF_YEAR) == calNow.get(java.util.Calendar.DAY_OF_YEAR));
            boolean isYesterday = isSameYear && (calMsg.get(java.util.Calendar.DAY_OF_YEAR) == calNow.get(java.util.Calendar.DAY_OF_YEAR) - 1);

            // 4. 決定顯示格式
            if (isSameDay) {
                // 如果是今天 -> 顯示 "16:20"
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                return timeFormat.format(msgDate);
            } else if (isYesterday) {
                // 如果是昨天 -> 顯示 "昨天" (或是 "昨天 16:20" 看你喜好)
                return "昨天";
            } else if (isSameYear) {
                // 如果是今年 -> 顯示 "12/28"
                java.text.SimpleDateFormat monthDayFormat = new java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault());
                return monthDayFormat.format(msgDate);
            } else {
                // 如果是跨年了 -> 顯示 "2024/12/31"
                java.text.SimpleDateFormat yearFormat = new java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault());
                return yearFormat.format(msgDate);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 如果解析失敗，回傳原本的簡單切割 (當作備案)
            if (rawTime.length() > 16) return rawTime.substring(11, 16);
            return rawTime;
        }
    }
}