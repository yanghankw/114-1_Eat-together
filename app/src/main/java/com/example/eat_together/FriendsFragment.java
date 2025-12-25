package com.example.eat_together;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friendList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // --- 移除 SearchView 底線的程式碼 ---
        SearchView searchView = view.findViewById(R.id.sv_location);
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plateView = searchView.findViewById(plateId);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
        // ------------------------------------

        recyclerView = view.findViewById(R.id.recycler_view_friends);

        // 設定列表呈現方式：垂直條列式
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 產生假資料 (這是目前的預設畫面，之後 Server 回傳資料後可以清空這裡再 add)
        friendList = new ArrayList<>();
        friendList.add(new Friend("王小明", "想吃燒肉", R.drawable.ic_person));
        friendList.add(new Friend("陳小美", "減肥中...", R.drawable.ic_person));
        friendList.add(new Friend("林大華", "約嗎？", R.drawable.ic_person));
        friendList.add(new Friend("測試帳號", "Offline", R.drawable.ic_person));

        // 綁定 Adapter
        adapter = new FriendsAdapter(friendList);
        recyclerView.setAdapter(adapter);

        // ==========================================
        // 🔥 關鍵新增：一開啟這個頁面，就跟 Server 要最新的好友名單
        // ==========================================
        // 指令格式範例: GET_FRIENDS
        // (如果有 User ID，通常會寫 GET_FRIENDS:1，這裡先簡化)
        TcpClient.getInstance().sendMessage("GET_FRIENDS");
        // ==========================================

        return view;
    }
}