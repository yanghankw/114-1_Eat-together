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

        // 1. 初始化空列表
        friendList = new ArrayList<>();
        adapter = new FriendsAdapter(friendList);
        recyclerView.setAdapter(adapter);

        // 2. ★ 關鍵：連線抓取所有用戶
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
            // 解析 JSON Array
            JSONArray jsonArray = new JSONArray(jsonString);
            List<Friend> newUsers = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObj = jsonArray.getJSONObject(i);

                // 假設 Supabase 的欄位是 username, email
                String name = userObj.optString("username", "未知用戶");
                String email = userObj.optString("email", "");

                // 建立物件 (暫用 Friend 物件顯示)
                newUsers.add(new Friend(name, email, R.drawable.ic_person));
            }

            // 3. 回到主執行緒更新 UI
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