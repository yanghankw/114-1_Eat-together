package com.example.eat_together;

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

public class FriendsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friendList; // 這裡暫時沿用 Friend 類別來存使用者資料

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // --- UI 美化設定 ---
        setupSearchView(view);

        recyclerView = view.findViewById(R.id.recycler_view_friends);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        // ★ 1. 從 SharedPreferences 讀取真實 ID
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("user_id", null);

        // 防呆：如果沒登入 (抓不到 ID)，最好提示一下或跳回登入頁
        if (currentUserId == null) {
            android.widget.Toast.makeText(getContext(), "尚未登入，無法加好友", android.widget.Toast.LENGTH_SHORT).show();
            // 這裡可以選擇 return view; 或是做其他處理
        }

        // 2. 初始化空列表
        friendList = new ArrayList<>();
        adapter = new FriendsAdapter(friendList, currentUserId);
        recyclerView.setAdapter(adapter);

        // 3. ★ 關鍵：連線抓取所有用戶
        loadAllUsers();

        return view;
    }

    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.sv_location);
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plateView = searchView.findViewById(plateId);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // --- 核心邏輯：向 Server 請求所有使用者名單 ---
    private void loadAllUsers() {
        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect(); // 確保連線

            // 1. 發送指令 (GET_ALL_USERS)
            // 記得 TcpClient 要有 sendRequest (發送並等待回應) 的方法
            // 如果只有 sendMessage，您可能需要改用 Handler 或 Broadcast 來接收回傳
            // 這裡假設您已經依上次教學加上了 sendRequest
            String response = client.sendRequest("GET_ALL_USERS");

            // 2. 解析回應
            if (response != null && response.startsWith("USERS_JSON:")) {
                String jsonString = response.substring("USERS_JSON:".length());
                updateListWithJson(jsonString);
            } else {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "無法取得用戶列表", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void updateListWithJson(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<Friend> newUsers = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObj = jsonArray.getJSONObject(i);

                // ★★★ 1. 這裡一定要抓 ID (之前可能漏了) ★★★
                String uuid = userObj.optString("id");

                String name = userObj.optString("username", "未知用戶");
                String email = userObj.optString("email", "");

                // ★★★ 2. 判斷 ID 存在才加入列表 ★★★
                if (uuid != null && !uuid.isEmpty()) {
                    newUsers.add(new Friend(uuid, name, email, R.drawable.ic_person));
                }
            }

            getActivity().runOnUiThread(() -> {
                friendList.clear();
                friendList.addAll(newUsers);
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}