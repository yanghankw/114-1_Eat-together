package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    selectedFragment = new HomeFragment();
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

        // 預設一開啟 App 顯示「找餐廳(首頁)」
        // 這裡您可以決定預設要顯示哪一頁，LINE 通常預設是好友或聊天
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home); // 這會觸發上面的監聽器
        }
    }
}