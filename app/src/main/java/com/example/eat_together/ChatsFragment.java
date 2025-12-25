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

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatsAdapter adapter;
    private List<ChatSession> chatList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // --- 移除 SearchView 底線的程式碼 ---
        SearchView searchView = view.findViewById(R.id.sv_location);
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plateView = searchView.findViewById(plateId);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
        // ------------------------------------

        recyclerView = view.findViewById(R.id.recycler_view_chats);

        // 設定列表呈現方式：垂直條列式
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 產生假資料 (未來這些資料會從您的 TCP Server 抓取)
        chatList = new ArrayList<>();
        chatList.add(new ChatSession("王小明", "想吃燒肉", "1000", R.drawable.ic_person));
        chatList.add(new ChatSession("陳小美", "減肥中...", "1000", R.drawable.ic_person));
        chatList.add(new ChatSession("林大華", "約嗎？", "1000", R.drawable.ic_person));
        chatList.add(new ChatSession("測試帳號", "Offline", "1000", R.drawable.ic_person));

        // 綁定 Adapter
        adapter = new ChatsAdapter(getContext(), chatList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}