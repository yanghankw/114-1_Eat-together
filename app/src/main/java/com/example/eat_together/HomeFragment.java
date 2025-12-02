package com.example.eat_together;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. 載入佈局檔
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 2. 綁定按鈕 (這裡是整個 LinearLayout 被當作按鈕)
        LinearLayout btnOpenMap = view.findViewById(R.id.btn_open_map);

        // 3. 設定點擊事件
        btnOpenMap.setOnClickListener(v -> {
            // 跳轉到 MapActivity
            Intent intent = new Intent(getActivity(), MapActivity.class);
            startActivity(intent);
        });

        return view;
    }
}