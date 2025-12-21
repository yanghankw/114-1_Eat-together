package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ==========================================
        // 🔥 關鍵新增：App 一啟動就自動連線 Server
        // ==========================================
        // 必須放在 Thread (執行緒) 裡面，因為 Android 禁止在主執行緒做網路連線
        new Thread(() -> {
            try {
                Log.d("MainActivity", "🚀 App 啟動，正在嘗試連線到 Server...");
                // 呼叫我們寫好的 TcpClient 單例來連線
                // 請確認 TcpClient.java 裡面的 IP 是電腦的 IP (192.168.x.x)
                TcpClient.getInstance().connect();
            } catch (Exception e) {
                Log.e("MainActivity", "❌ 連線發生錯誤", e);
            }
        }).start();
        // ==========================================


        // --- 以下是你原本的底部導覽列邏輯 ---
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 設定點擊監聽器
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                // 根據 ID 判斷點了哪個按鈕
                int itemId = item.getItemId();

                if (itemId == R.id.nav_friends) {
                    selectedFragment = new FriendsFragment();
                } else if (itemId == R.id.nav_chats) {
                    selectedFragment = new ChatsFragment();
                } else if (itemId == R.id.nav_home) {
                    // 直接啟動 MapActivity
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                // 切換 Fragment
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            }
        });

        // 預設一開啟 App 顯示「好友列表」
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_friends);
        }
    }
}