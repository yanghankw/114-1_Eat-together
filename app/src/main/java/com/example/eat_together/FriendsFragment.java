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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friendList;
    private String currentUserId; // 用來存當前使用者的 ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        setupSearchView(view);

        recyclerView = view.findViewById(R.id.recycler_view_friends);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. 取得登入者的 ID
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "尚未登入，無法加好友", Toast.LENGTH_SHORT).show();
        }

        // 2. 初始化
        friendList = new ArrayList<>();
        adapter = new FriendsAdapter(friendList, currentUserId);
        recyclerView.setAdapter(adapter);

        // 3. 載入資料 (同時載入全部用戶與好友名單)
        loadData();

        return view;
    }

    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.sv_location);
        if (searchView != null) {
            int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
            View plateView = searchView.findViewById(plateId);
            if (plateView != null) {
                plateView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    // --- 核心邏輯：抓兩份資料並比對 ---
    private void loadData() {
        if (currentUserId == null) return;

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect();

            // ★ 步驟 A: 獲取所有用戶
            // 預期回傳: USERS_JSON:[{"id":"...", "username":"..."}, ...]
            String allUsersResponse = client.sendRequest("GET_ALL_USERS");

            // ★ 步驟 B: 獲取我的好友 ID 列表
            // 預期回傳: MY_FRIENDS_JSON:["uuid-1", "uuid-2", ...]
            // 您需要在 Server 端實作處理 "GET_MY_FRIENDS:<user_id>" 的指令
            String myFriendsResponse = client.sendRequest("GET_MY_FRIENDS:" + currentUserId);

            // 解析並更新 UI
            processResponses(allUsersResponse, myFriendsResponse);

        }).start();
    }

    private void processResponses(String allUsersRaw, String myFriendsRaw) {
        try {
            // 1. 解析所有用戶
            JSONArray allUsersArray = new JSONArray();
            if (allUsersRaw != null && allUsersRaw.startsWith("USERS_JSON:")) {
                allUsersArray = new JSONArray(allUsersRaw.substring("USERS_JSON:".length()));
            }

            // 2. 解析我的好友 ID (放入 Set 以便快速查詢)
            Set<String> friendIds = new HashSet<>();
            if (myFriendsRaw != null && myFriendsRaw.startsWith("MY_FRIENDS_JSON:")) {
                JSONArray friendsArray = new JSONArray(myFriendsRaw.substring("MY_FRIENDS_JSON:".length()));
                for (int i = 0; i < friendsArray.length(); i++) {
                    // 假設回傳的是 ID 字串陣列 ["id1", "id2"]
                    friendIds.add(friendsArray.getString(i));

                    // 如果 Server 回傳的是物件陣列 [{"friend_id":"id1"}], 則改為:
                    // friendIds.add(friendsArray.getJSONObject(i).optString("friend_id"));
                }
            }

            List<Friend> newUsers = new ArrayList<>();

            for (int i = 0; i < allUsersArray.length(); i++) {
                JSONObject userObj = allUsersArray.getJSONObject(i);
                String uuid = userObj.optString("id");
                String name = userObj.optString("username", "未知用戶");
                String email = userObj.optString("email", "");

                // 排除自己
                if (uuid.equals(currentUserId)) continue;

                if (uuid != null && !uuid.isEmpty()) {
                    // ★ 比對：這個人是否在好友名單中
                    boolean isAlreadyFriend = friendIds.contains(uuid);

                    // 建立物件時傳入狀態
                    newUsers.add(new Friend(uuid, name, email, R.drawable.ic_person, isAlreadyFriend));
                }
            }

            // 回到主執行緒更新畫面
            getActivity().runOnUiThread(() -> {
                friendList.clear();
                friendList.addAll(newUsers);
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "資料解析錯誤", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
