package com.example.eat_together;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatsAdapter adapter;
    private List<ChatSession> chatList;
    private String myId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // 1. 讀取使用者 ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        myId = prefs.getString("user_id", null);

        // 2. 初始化 UI 元件
        setupSearchView(view);
        setupRecyclerView(view);
        setupButtons(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到這個頁面時，重新整理列表 (確保最後訊息是最新的)
        if (myId != null) {
            loadAllChats();
        } else {
            Toast.makeText(getContext(), "請先登入", Toast.LENGTH_SHORT).show();
        }
    }

    // --- UI 初始化區 ---

    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.sv_location);
        // 去除搜尋框底線背景 (您原本的美化設定)
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plateView = searchView.findViewById(plateId);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatList = new ArrayList<>();
        adapter = new ChatsAdapter(getContext(), chatList);
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons(View view) {
        Button btnCreateGroup = view.findViewById(R.id.btn_test_group);
        btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    // --- 核心功能：建立群組 ---

    private void showCreateGroupDialog() {
        final EditText input = new EditText(getContext());
        input.setHint("請輸入群組名稱");

        new AlertDialog.Builder(getContext())
                .setTitle("建立新群組")
                .setView(input)
                .setPositiveButton("建立", (dialog, which) -> {
                    String groupName = input.getText().toString().trim();
                    if (!groupName.isEmpty()) {
                        createGroupOnServer(groupName);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void createGroupOnServer(String groupName) {
        if (myId == null) return;

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (client.isConnected()) {
                // 發送指令: CREATE_GROUP:群組名:創建者ID
                String response = client.sendRequest("CREATE_GROUP:" + groupName + ":" + myId);

                // 切回主執行緒更新 UI
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response != null && response.startsWith("CREATE_GROUP_SUCCESS:")) {
                            Toast.makeText(getContext(), "群組建立成功！", Toast.LENGTH_SHORT).show();
                            loadAllChats(); // 建立成功後，重新讀取列表
                        } else {
                            Toast.makeText(getContext(), "建立失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // --- 核心功能：載入列表 (整合好友與群組) ---

    private void loadAllChats() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            if (!client.isConnected()) client.connect();

            // 建立一個暫存的 List，全部抓完再一次更新，避免畫面閃爍
            List<ChatSession> tempAllChats = new ArrayList<>();

            // 1. 抓取好友 (PRIVATE)
            String friendResponse = client.sendRequest("GET_FRIENDS:" + myId);
            if (friendResponse != null && friendResponse.startsWith("FRIENDS_JSON:")) {
                String jsonStr = friendResponse.substring("FRIENDS_JSON:".length());
                tempAllChats.addAll(parseChats(jsonStr, "PRIVATE"));
            }

            // 2. 抓取群組 (GROUP)
            String groupResponse = client.sendRequest("GET_MY_GROUPS:" + myId);
            if (groupResponse != null && groupResponse.startsWith("GROUPS_JSON:")) {
                String jsonStr = groupResponse.substring("GROUPS_JSON:".length());
                tempAllChats.addAll(parseChats(jsonStr, "GROUP"));
            }

            // 3. 統一更新 UI
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    chatList.clear();
                    chatList.addAll(tempAllChats);
                    adapter.notifyDataSetChanged();
                });
            }

        }).start();
    }

    // 通用的解析方法
    private List<ChatSession> parseChats(String jsonString, String type) {
        List<ChatSession> resultList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String id;
                String name;
                String lastMsg = "點擊開始聊天"; // 預設值
                String time = "";
                int iconRes;

                if ("PRIVATE".equals(type)) {
                    // --- 私聊解析 ---
                    name = obj.optString("friend_name", "未知好友");
                    id = obj.optString("friend_id");
                    iconRes = R.drawable.ic_person; // 需確保此圖示存在
                } else {
                    // --- 群組解析 ---
                    id = String.valueOf(obj.optInt("group_id"));
                    name = obj.optString("group_name", "群組 " + id);
                    iconRes = R.drawable.ic_launcher_foreground; // 群組圖示
                }

                // --- 共同欄位 (最後訊息與時間) ---
                if (!obj.isNull("last_msg")) {
                    lastMsg = obj.getString("last_msg");
                }

                if (!obj.isNull("last_time")) {
                    String rawTime = obj.getString("last_time");
                    time = formatDisplayTime(rawTime);
                }

                resultList.add(new ChatSession(id, name, lastMsg, time, iconRes, type));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    // --- 工具：時間格式化 ---

    private String formatDisplayTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) return "";
        try {
            // 處理時間字串 (移除 T 和 毫秒)
            String cleanTime = rawTime.replace("T", " ");
            if (cleanTime.contains(".")) {
                cleanTime = cleanTime.substring(0, cleanTime.indexOf("."));
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date msgDate = sdf.parse(cleanTime);
            Date now = new Date();

            Calendar calMsg = Calendar.getInstance();
            calMsg.setTime(msgDate);
            Calendar calNow = Calendar.getInstance();
            calNow.setTime(now);

            boolean isSameYear = calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR);
            boolean isSameDay = isSameYear && (calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR));
            boolean isYesterday = isSameYear && (calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) - 1);

            if (isSameDay) {
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(msgDate);
            } else if (isYesterday) {
                return "昨天";
            } else if (isSameYear) {
                return new SimpleDateFormat("MM/dd", Locale.getDefault()).format(msgDate);
            } else {
                return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(msgDate);
            }
        } catch (Exception e) {
            // 解析失敗時，回傳簡單擷取的字串作為備案
            return rawTime.length() > 16 ? rawTime.substring(0, 10) : rawTime;
        }
    }
}